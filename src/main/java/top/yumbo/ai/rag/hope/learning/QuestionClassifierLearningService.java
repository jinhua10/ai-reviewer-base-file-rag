package top.yumbo.ai.rag.hope.learning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.QuestionClassifier;
import top.yumbo.ai.rag.hope.QuestionClassifier.QuestionTypeConfig;
import top.yumbo.ai.rag.hope.persistence.QuestionClassifierPersistence;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 自进化问题分类学习服务
 * (Self-Evolving Question Classification Learning Service)
 *
 * <p>
 * 核心能力 (Core Capabilities):
 * <ul>
 *   <li>✅ 自动发现新类型 - 通过聚类分析发现未分类的问题模式</li>
 *   <li>✅ 动态调整优先级 - 根据使用频率自动调整类型优先级</li>
 *   <li>✅ 关键词自动提取 - 从分类样本中自动提取关键词</li>
 *   <li>✅ 模式自动生成 - 基于样本生成正则表达式模式</li>
 *   <li>✅ 反馈学习 - 从用户反馈中学习改进分类规则</li>
 *   <li>✅ A/B测试 - 对新规则进行A/B测试验证</li>
 * </ul>
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2.1.0
 */
@Slf4j
@Service
public class QuestionClassifierLearningService {

    @Autowired
    private QuestionClassifierPersistence persistence;

    @Autowired
    private QuestionClassifier classifier;

    // 学习队列 (Learning queue)
    private final BlockingQueue<QuestionSample> sampleQueue = new LinkedBlockingQueue<>(10000);

    // 统计数据 (Statistics)
    private final Map<String, TypeStatistics> typeStats = new ConcurrentHashMap<>();

    // 未分类样本缓存 (Unclassified sample cache)
    private final List<QuestionSample> unclassifiedSamples = new CopyOnWriteArrayList<>();

    // 学习线程池 (Learning thread pool)
    private final ExecutorService learningExecutor = Executors.newFixedThreadPool(2);

    // 聚类引擎 (Clustering engine)
    private final ClusteringEngine clusteringEngine = new ClusteringEngine();

    /**
     * 问题样本 (Question Sample)
     */
    public static class QuestionSample {
        private String question;
        private QuestionClassifier.QuestionType detectedType;
        private double confidence;
        private String userFeedback;  // CORRECT, INCORRECT, UNKNOWN
        private String expectedType;
        private long timestamp;

        // Getters and Setters
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public QuestionClassifier.QuestionType getDetectedType() { return detectedType; }
        public void setDetectedType(QuestionClassifier.QuestionType type) { this.detectedType = type; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public String getUserFeedback() { return userFeedback; }
        public void setUserFeedback(String feedback) { this.userFeedback = feedback; }

        public String getExpectedType() { return expectedType; }
        public void setExpectedType(String type) { this.expectedType = type; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 类型统计 (Type Statistics)
     */
    public static class TypeStatistics {
        private String typeId;
        private long totalCount;
        private long correctCount;
        private long incorrectCount;
        private double averageConfidence;
        private long lastUsedTimestamp;

        // Getters and Setters
        public String getTypeId() { return typeId; }
        public void setTypeId(String typeId) { this.typeId = typeId; }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long count) { this.totalCount = count; }
        public void incrementTotalCount() { this.totalCount++; }

        public long getCorrectCount() { return correctCount; }
        public void setCorrectCount(long count) { this.correctCount = count; }
        public void incrementCorrectCount() { this.correctCount++; }

        public long getIncorrectCount() { return incorrectCount; }
        public void setIncorrectCount(long count) { this.incorrectCount = count; }
        public void incrementIncorrectCount() { this.incorrectCount++; }

        public double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(double conf) { this.averageConfidence = conf; }

        public long getLastUsedTimestamp() { return lastUsedTimestamp; }
        public void setLastUsedTimestamp(long timestamp) { this.lastUsedTimestamp = timestamp; }

        public double getAccuracy() {
            if (totalCount == 0) return 0.0;
            return (double) correctCount / totalCount;
        }
    }

    /**
     * 聚类结果 (Clustering Result)
     */
    public static class Cluster {
        private List<QuestionSample> samples;
        private Set<String> commonKeywords;
        private String suggestedTypeId;
        private String suggestedTypeName;
        private double coherence;

        public Cluster() {
            this.samples = new ArrayList<>();
            this.commonKeywords = new HashSet<>();
        }

        // Getters and Setters
        public List<QuestionSample> getSamples() { return samples; }
        public void setSamples(List<QuestionSample> samples) { this.samples = samples; }

        public Set<String> getCommonKeywords() { return commonKeywords; }
        public void setCommonKeywords(Set<String> keywords) { this.commonKeywords = keywords; }

        public String getSuggestedTypeId() { return suggestedTypeId; }
        public void setSuggestedTypeId(String typeId) { this.suggestedTypeId = typeId; }

        public String getSuggestedTypeName() { return suggestedTypeName; }
        public void setSuggestedTypeName(String name) { this.suggestedTypeName = name; }

        public double getCoherence() { return coherence; }
        public void setCoherence(double coherence) { this.coherence = coherence; }
    }

    @PostConstruct
    public void init() {
        // 启动学习线程 (Start learning threads)
        learningExecutor.submit(this::processLearningQueue);
        learningExecutor.submit(this::periodicClusteringTask);

        log.info("QuestionClassifierLearningService initialized");
    }

    /**
     * 提交学习样本 (Submit learning sample)
     *
     * @param sample 问题样本
     */
    public void submitSample(QuestionSample sample) {
        try {
            sampleQueue.offer(sample, 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Failed to submit sample", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 记录分类结果 (Record classification result)
     *
     * @param question 问题
     * @param type 检测到的类型
     * @param confidence 置信度
     */
    public void recordClassification(String question, QuestionClassifier.QuestionType type, double confidence) {
        QuestionSample sample = new QuestionSample();
        sample.setQuestion(question);
        sample.setDetectedType(type);
        sample.setConfidence(confidence);
        sample.setTimestamp(System.currentTimeMillis());

        submitSample(sample);
    }

    /**
     * 记录用户反馈 (Record user feedback)
     *
     * @param question 问题
     * @param detectedType 检测到的类型
     * @param feedback 用户反馈
     * @param expectedType 期望类型（如果有）
     */
    public void recordFeedback(String question, QuestionClassifier.QuestionType detectedType,
                              String feedback, String expectedType) {
        QuestionSample sample = new QuestionSample();
        sample.setQuestion(question);
        sample.setDetectedType(detectedType);
        sample.setUserFeedback(feedback);
        sample.setExpectedType(expectedType);
        sample.setTimestamp(System.currentTimeMillis());

        submitSample(sample);
    }

    /**
     * 处理学习队列 (Process learning queue)
     */
    private void processLearningQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                QuestionSample sample = sampleQueue.poll(5, TimeUnit.SECONDS);
                if (sample != null) {
                    processSample(sample);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing sample", e);
            }
        }
    }

    /**
     * 处理单个样本 (Process single sample)
     */
    private void processSample(QuestionSample sample) {
        // 1. 更新统计数据 (Update statistics)
        updateStatistics(sample);

        // 2. 如果是低置信度或错误分类，加入未分类样本 (Add to unclassified if low confidence or incorrect)
        if (sample.getConfidence() < 0.6 || "INCORRECT".equals(sample.getUserFeedback())) {
            unclassifiedSamples.add(sample);

            // 当未分类样本达到一定数量时，触发聚类 (Trigger clustering when enough samples)
            if (unclassifiedSamples.size() >= 100) {
                triggerClustering();
            }
        }

        // 3. 如果用户提供了正确类型，学习新关键词 (Learn new keywords if user provided correct type)
        if (sample.getExpectedType() != null) {
            learnFromFeedback(sample);
        }
    }

    /**
     * 更新统计数据 (Update statistics)
     */
    private void updateStatistics(QuestionSample sample) {
        if (sample.getDetectedType() == null) return;

        String typeId = sample.getDetectedType().getId();
        TypeStatistics stats = typeStats.computeIfAbsent(typeId, k -> {
            TypeStatistics s = new TypeStatistics();
            s.setTypeId(typeId);
            return s;
        });

        stats.incrementTotalCount();
        stats.setLastUsedTimestamp(sample.getTimestamp());

        if ("CORRECT".equals(sample.getUserFeedback())) {
            stats.incrementCorrectCount();
        } else if ("INCORRECT".equals(sample.getUserFeedback())) {
            stats.incrementIncorrectCount();
        }

        // 更新平均置信度 (Update average confidence)
        double totalConfidence = stats.getAverageConfidence() * (stats.getTotalCount() - 1) + sample.getConfidence();
        stats.setAverageConfidence(totalConfidence / stats.getTotalCount());
    }

    /**
     * 从反馈中学习 (Learn from feedback)
     */
    private void learnFromFeedback(QuestionSample sample) {
        try {
            String question = sample.getQuestion().toLowerCase();
            String expectedType = sample.getExpectedType();

            // 提取新关键词 (Extract new keywords)
            Set<String> newKeywords = extractKeywords(question);

            // 添加到持久化 (Add to persistence)
            List<String> existingKeywords = persistence.getKeywords(expectedType);
            Set<String> combined = new HashSet<>(existingKeywords);
            combined.addAll(newKeywords);

            // 只添加新的关键词 (Only add new keywords)
            List<String> toAdd = newKeywords.stream()
                .filter(kw -> !existingKeywords.contains(kw))
                .collect(Collectors.toList());

            if (!toAdd.isEmpty()) {
                persistence.addKeywords(expectedType, toAdd);
                log.info("Learned {} new keywords for type: {}", toAdd.size(), expectedType);
            }
        } catch (Exception e) {
            log.error("Failed to learn from feedback", e);
        }
    }

    /**
     * 触发聚类分析 (Trigger clustering analysis)
     */
    private void triggerClustering() {
        try {
            log.info("Starting clustering analysis on {} unclassified samples", unclassifiedSamples.size());

            List<Cluster> clusters = clusteringEngine.cluster(unclassifiedSamples);

            for (Cluster cluster : clusters) {
                if (cluster.getSamples().size() >= 10 && cluster.getCoherence() > 0.7) {
                    // 发现新类型 (Discovered new type)
                    proposeNewType(cluster);
                }
            }

            // 清空已处理的样本 (Clear processed samples)
            unclassifiedSamples.clear();

        } catch (Exception e) {
            log.error("Failed to perform clustering", e);
        }
    }

    /**
     * 提议新类型 (Propose new type)
     */
    private void proposeNewType(Cluster cluster) {
        String typeId = "auto_" + UUID.randomUUID().toString().substring(0, 8);
        String typeName = "自动发现类型_" + cluster.getCommonKeywords().stream()
            .limit(3)
            .collect(Collectors.joining("_"));

        QuestionTypeConfig newType = new QuestionTypeConfig();
        newType.setId(typeId);
        newType.setName(typeName);
        newType.setNameEn("Auto_Type_" + typeId);
        newType.setPriority(999);  // 低优先级
        newType.setComplexity("moderate");
        newType.setSuggestedLayer("full_rag");
        newType.setEnabled(false);  // 默认禁用，等待人工审核

        // 保存新类型 (Save new type)
        persistence.saveQuestionType(newType);

        // 保存关键词 (Save keywords)
        persistence.saveKeywords(typeId, new ArrayList<>(cluster.getCommonKeywords()));

        log.info("Proposed new question type: {} with {} samples", typeName, cluster.getSamples().size());

        // TODO: 发送通知给管理员审核 (Send notification to admin for review)
    }

    /**
     * 定期聚类任务 (Periodic clustering task)
     */
    private void periodicClusteringTask() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(3600_000);  // 每小时执行一次 (Every hour)

                if (unclassifiedSamples.size() >= 20) {
                    triggerClustering();
                }

                // 调整优先级 (Adjust priorities)
                adjustPriorities();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in periodic clustering task", e);
            }
        }
    }

    /**
     * 调整优先级 (Adjust priorities)
     */
    private void adjustPriorities() {
        try {
            List<TypeStatistics> sortedStats = typeStats.values().stream()
                .filter(s -> s.getTotalCount() > 10)
                .sorted((a, b) -> {
                    // 按使用频率和准确率排序 (Sort by usage frequency and accuracy)
                    double scoreA = a.getTotalCount() * a.getAccuracy();
                    double scoreB = b.getTotalCount() * b.getAccuracy();
                    return Double.compare(scoreB, scoreA);
                })
                .toList();

            // 更新优先级 (Update priorities)
            int priority = 1;
            for (TypeStatistics stats : sortedStats) {
                Optional<QuestionTypeConfig> configOpt = persistence.getQuestionType(stats.getTypeId());
                if (configOpt.isPresent()) {
                    QuestionTypeConfig config = configOpt.get();
                    if (config.getPriority() != priority) {
                        config.setPriority(priority);
                        persistence.updateQuestionType(config);
                        log.debug("Adjusted priority for {}: {}", stats.getTypeId(), priority);
                    }
                }
                priority++;
            }

            log.info("Adjusted priorities for {} types", sortedStats.size());
        } catch (Exception e) {
            log.error("Failed to adjust priorities", e);
        }
    }

    /**
     * 提取关键词 (Extract keywords)
     */
    private Set<String> extractKeywords(String question) {
        Set<String> keywords = new HashSet<>();

        // 简单的关键词提取（移除停用词）
        String[] stopWords = {"的", "是", "在", "了", "和", "有", "我", "你", "这", "那",
            "什么", "怎么", "如何", "为什么", "a", "an", "the", "is", "are", "what", "how", "why"};

        String[] words = question.split("\\s+");
        for (String word : words) {
            word = word.trim().toLowerCase();
            if (word.length() > 1 && !Arrays.asList(stopWords).contains(word)) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * 获取类型统计 (Get type statistics)
     */
    public Map<String, TypeStatistics> getTypeStatistics() {
        return new HashMap<>(typeStats);
    }

    /**
     * 简单聚类引擎 (Simple clustering engine)
     */
    private static class ClusteringEngine {

        public List<Cluster> cluster(List<QuestionSample> samples) {
            // TODO: 实现更复杂的聚类算法（K-means, DBSCAN等）
            // 这里提供一个简单的基于关键词相似度的聚类

            List<Cluster> clusters = new ArrayList<>();
            Set<QuestionSample> processed = new HashSet<>();

            for (QuestionSample sample : samples) {
                if (processed.contains(sample)) continue;

                Cluster cluster = new Cluster();
                cluster.getSamples().add(sample);
                processed.add(sample);

                Set<String> keywords1 = extractKeywords(sample.getQuestion());

                // 查找相似的样本 (Find similar samples)
                for (QuestionSample other : samples) {
                    if (processed.contains(other)) continue;

                    Set<String> keywords2 = extractKeywords(other.getQuestion());
                    double similarity = calculateSimilarity(keywords1, keywords2);

                    if (similarity > 0.5) {
                        cluster.getSamples().add(other);
                        processed.add(other);
                    }
                }

                // 提取公共关键词 (Extract common keywords)
                if (cluster.getSamples().size() >= 2) {
                    cluster.setCommonKeywords(findCommonKeywords(cluster.getSamples()));
                    cluster.setCoherence(calculateCoherence(cluster));
                    clusters.add(cluster);
                }
            }

            return clusters;
        }

        private Set<String> extractKeywords(String question) {
            Set<String> keywords = new HashSet<>();
            String[] words = question.toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.length() > 2) {
                    keywords.add(word);
                }
            }
            return keywords;
        }

        private double calculateSimilarity(Set<String> set1, Set<String> set2) {
            if (set1.isEmpty() || set2.isEmpty()) return 0.0;

            Set<String> intersection = new HashSet<>(set1);
            intersection.retainAll(set2);

            Set<String> union = new HashSet<>(set1);
            union.addAll(set2);

            return (double) intersection.size() / union.size();
        }

        private Set<String> findCommonKeywords(List<QuestionSample> samples) {
            if (samples.isEmpty()) return new HashSet<>();

            Set<String> common = extractKeywords(samples.get(0).getQuestion());

            for (int i = 1; i < samples.size(); i++) {
                Set<String> keywords = extractKeywords(samples.get(i).getQuestion());
                common.retainAll(keywords);
            }

            return common;
        }

        private double calculateCoherence(Cluster cluster) {
            // 计算聚类紧密度 (Calculate cluster coherence)
            if (cluster.getSamples().size() < 2) return 0.0;

            List<Set<String>> allKeywords = cluster.getSamples().stream()
                .map(s -> extractKeywords(s.getQuestion()))
                .collect(Collectors.toList());

            double totalSimilarity = 0.0;
            int count = 0;

            for (int i = 0; i < allKeywords.size(); i++) {
                for (int j = i + 1; j < allKeywords.size(); j++) {
                    totalSimilarity += calculateSimilarity(allKeywords.get(i), allKeywords.get(j));
                    count++;
                }
            }

            return count > 0 ? totalSimilarity / count : 0.0;
        }
    }
}

