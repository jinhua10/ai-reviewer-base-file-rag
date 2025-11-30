package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.service.LocalFileRAG;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 相似问题检测服务
 * 基于向量检索在归档的问答中查找相似问题
 *
 * @author AI Reviewer Team
 * @since 2025-11-30
 */
@Slf4j
@Service
public class SimilarQAService {

    private static final Pattern QUESTION_PATTERN = Pattern.compile("^question:\\s*\"(.+?)\"", Pattern.MULTILINE);
    private static final Pattern RATING_PATTERN = Pattern.compile("^rating:\\s*(\\d+)", Pattern.MULTILINE);
    private static final Pattern ANSWER_PATTERN = Pattern.compile("## 回答\\s*\\n\\s*\\n(.+?)(?=\\n##|$)", Pattern.DOTALL);

    private final LocalEmbeddingEngine embeddingEngine;
    private final SimpleVectorIndexEngine vectorIndexEngine;
    private final LocalFileRAG rag;

    @Autowired
    public SimilarQAService(LocalEmbeddingEngine embeddingEngine,
                           SimpleVectorIndexEngine vectorIndexEngine,
                           LocalFileRAG rag) {
        this.embeddingEngine = embeddingEngine;
        this.vectorIndexEngine = vectorIndexEngine;
        this.rag = rag;
    }

    /**
     * 查找相似问题
     *
     * @param question  用户问题
     * @param threshold 相似度阈值（0.0-1.0）
     * @param limit     返回数量上限
     * @return 相似问题列表
     */
    public List<SimilarQA> findSimilar(String question, float threshold, int limit) {
        try {
            // 1. 生成问题向量
            float[] queryVector = embeddingEngine.embed(question);

            // 2. 向量检索归档文档
            List<SimpleVectorIndexEngine.VectorSearchResult> searchResults =
                    vectorIndexEngine.search(queryVector, limit * 2, threshold); // 多取一些候选

            // 3. 过滤并解析
            List<SimilarQA> similarQAs = new ArrayList<>();

            for (var result : searchResults) {
                // 获取文档内容（使用 LocalFileRAG）
                Document doc = rag.getDocument(result.getDocId());
                if (doc == null) {
                    continue;
                }

                // 解析文档（假设是归档的Markdown）
                SimilarQA qa = parseArchivedQA(doc);
                if (qa == null) {
                    continue;
                }

                // 只返回高质量问答（评分>=4星）
                if (qa.getRating() >= 4) {
                    qa.setSimilarity(result.getSimilarity());
                    similarQAs.add(qa);
                }

                if (similarQAs.size() >= limit) {
                    break;
                }
            }

            // 按相似度降序排序
            similarQAs.sort(Comparator.comparing(SimilarQA::getSimilarity).reversed());

            log.info("🔍 Found {} similar questions for: {}", similarQAs.size(), question);

            return similarQAs;

        } catch (Exception e) {
            log.error("❌ Failed to find similar questions", e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析归档的问答Markdown文档
     */
    private SimilarQA parseArchivedQA(Document doc) {
        try {
            String content = doc.getContent();

            // 提取问题
            Matcher questionMatcher = QUESTION_PATTERN.matcher(content);
            if (!questionMatcher.find()) {
                return null;
            }
            String question = questionMatcher.group(1);

            // 提取评分
            Matcher ratingMatcher = RATING_PATTERN.matcher(content);
            int rating = ratingMatcher.find() ? Integer.parseInt(ratingMatcher.group(1)) : 0;

            // 提取回答
            Matcher answerMatcher = ANSWER_PATTERN.matcher(content);
            String answer = answerMatcher.find() ? answerMatcher.group(1).trim() : "";

            // 提取文档ID
            String docId = doc.getId();

            SimilarQA qa = new SimilarQA();
            qa.setQuestion(question);
            qa.setAnswer(answer);
            qa.setRating(rating);
            qa.setDocumentId(docId);
            qa.setDocumentTitle(doc.getTitle());

            return qa;

        } catch (Exception e) {
            log.warn("⚠️ Failed to parse archived QA: {}", doc.getTitle(), e);
            return null;
        }
    }

    /**
     * 相似问答结果
     */
    @Data
    public static class SimilarQA {
        /**
         * 原始问题
         */
        private String question;

        /**
         * 回答内容
         */
        private String answer;

        /**
         * 评分（1-5）
         */
        private int rating;

        /**
         * 文档ID
         */
        private String documentId;

        /**
         * 文档标题
         */
        private String documentTitle;

        /**
         * 相似度（0.0-1.0）
         */
        private float similarity;
    }
}

