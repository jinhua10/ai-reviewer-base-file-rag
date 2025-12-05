package top.yumbo.ai.rag.spring.boot.service.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSource;
import top.yumbo.ai.rag.spring.boot.model.document.MemoEntry;
import top.yumbo.ai.rag.spring.boot.model.document.SegmentType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 备忘录文档导出器
 *
 * 支持导出为多种格式：Markdown、JSON、HTML
 */
@Slf4j
@Service
public class MemoDocumentExporter {

    private final MemoAggregator aggregator;
    private final ObjectMapper objectMapper;

    @Autowired
    public MemoDocumentExporter(MemoAggregator aggregator) {
        this.aggregator = aggregator;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 导出为 Markdown 格式
     */
    public String exportToMarkdown(DocumentSource source,
                                   List<MemoEntry> shortTermMemory,
                                   List<MemoEntry> longTermMemo,
                                   String question) {
        StringBuilder md = new StringBuilder();

        // 标题
        md.append("# 📚 文档分析备忘录\n\n");

        // 元信息
        md.append("## 📋 文档信息\n\n");
        if (source != null) {
            md.append("| 属性 | 值 |\n");
            md.append("|------|----|\n");
            md.append("| 文档名称 | ").append(source.getDocumentName()).append(" |\n");
            md.append("| 文档类型 | ").append(source.getDocumentType()).append(" |\n");
            md.append("| 总片段数 | ").append(source.getTotalSegments()).append(" |\n");
            md.append("| 生成时间 | ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(" |\n");
            md.append("\n");
        }

        if (question != null && !question.isEmpty()) {
            md.append("**分析问题**: ").append(question).append("\n\n");
        }

        md.append("---\n\n");

        // 按主题聚合的内容
        List<MemoEntry> allMemos = new ArrayList<>();
        allMemos.addAll(longTermMemo);
        allMemos.addAll(shortTermMemory);

        List<MemoAggregator.TopicGroup> topicGroups = aggregator.aggregateByTopic(allMemos);

        if (!topicGroups.isEmpty()) {
            md.append("## 🏷️ 主题概览\n\n");

            for (MemoAggregator.TopicGroup group : topicGroups) {
                String emoji = getTopicEmoji(group.getTopic());
                md.append("### ").append(emoji).append(" ").append(group.getTopic()).append("\n\n");
                md.append("> 包含 ").append(group.getEntryCount()).append(" 个相关内容，重要性: ");
                md.append(formatImportance(group.getImportance())).append("\n\n");

                for (MemoEntry entry : group.getEntries()) {
                    md.append("#### 第 ").append(entry.getSegmentIndex()).append(" 部分");
                    if (entry.getTitle() != null && !entry.getTitle().isEmpty()) {
                        md.append(": ").append(entry.getTitle());
                    }
                    md.append("\n\n");

                    if (entry.isIndependent()) {
                        md.append("⭐ **独立重要条目**\n\n");
                    }

                    md.append(entry.getEffectiveContent()).append("\n\n");

                    if (entry.getKeywords() != null && !entry.getKeywords().isEmpty()) {
                        md.append("**关键词**: ");
                        md.append(String.join(", ", entry.getKeywords())).append("\n\n");
                    }
                }
            }
        }

        md.append("---\n\n");

        // 时间线视图
        md.append("## 📅 时间线视图\n\n");

        List<MemoEntry> sortedMemos = new ArrayList<>(allMemos);
        sortedMemos.sort(Comparator.comparingInt(MemoEntry::getSegmentIndex));

        for (MemoEntry entry : sortedMemos) {
            md.append("**[").append(entry.getSegmentIndex()).append("]** ");
            if (entry.getTitle() != null) {
                md.append(entry.getTitle());
            }
            md.append("\n");

            String content = entry.getEffectiveContent();
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            md.append("> ").append(content != null ? content.replace("\n", "\n> ") : "").append("\n\n");
        }

        // 统计信息
        md.append("---\n\n");
        md.append("## 📊 统计信息\n\n");
        md.append("| 指标 | 值 |\n");
        md.append("|------|----|\n");
        md.append("| 总条目数 | ").append(allMemos.size()).append(" |\n");
        md.append("| 短期记忆 | ").append(shortTermMemory.size()).append(" |\n");
        md.append("| 长期备忘录 | ").append(longTermMemo.size()).append(" |\n");
        md.append("| 独立重要条目 | ").append(allMemos.stream().filter(MemoEntry::isIndependent).count()).append(" |\n");
        md.append("| 主题数 | ").append(topicGroups.size()).append(" |\n");

        return md.toString();
    }

    /**
     * 导出为 JSON 格式
     */
    public String exportToJson(DocumentSource source,
                               List<MemoEntry> shortTermMemory,
                               List<MemoEntry> longTermMemo,
                               String question) {
        try {
            ExportData data = new ExportData();
            data.setSource(source);
            data.setQuestion(question);
            data.setShortTermMemory(shortTermMemory);
            data.setLongTermMemo(longTermMemo);
            data.setExportTime(LocalDateTime.now().toString());

            // 添加聚合信息
            List<MemoEntry> allMemos = new ArrayList<>();
            allMemos.addAll(longTermMemo);
            allMemos.addAll(shortTermMemory);

            data.setTopicGroups(aggregator.aggregateByTopic(allMemos));
            data.setTypeGroups(aggregator.aggregateByType(allMemos));

            // 统计信息
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEntries", allMemos.size());
            stats.put("shortTermCount", shortTermMemory.size());
            stats.put("longTermCount", longTermMemo.size());
            stats.put("independentCount", allMemos.stream().filter(MemoEntry::isIndependent).count());
            data.setStats(stats);

            return objectMapper.writeValueAsString(data);

        } catch (Exception e) {
            log.error("导出 JSON 失败", e);
            return "{}";
        }
    }

    /**
     * 导出为 HTML 格式
     */
    public String exportToHtml(DocumentSource source,
                               List<MemoEntry> shortTermMemory,
                               List<MemoEntry> longTermMemo,
                               String question) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>文档分析备忘录</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; }\n");
        html.append("    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; }\n");
        html.append("    .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 8px; }\n");
        html.append("    .entry { margin: 10px 0; padding: 10px; background: #f9f9f9; border-radius: 5px; }\n");
        html.append("    .important { border-left: 4px solid #f1c40f; }\n");
        html.append("    .keyword { display: inline-block; background: #3498db; color: white; padding: 2px 8px; border-radius: 3px; margin: 2px; font-size: 12px; }\n");
        html.append("    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }\n");
        html.append("    .stat-card { background: #ecf0f1; padding: 15px; border-radius: 8px; text-align: center; }\n");
        html.append("    .stat-value { font-size: 24px; font-weight: bold; color: #2c3e50; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("<div class=\"header\">\n");
        html.append("  <h1>📚 文档分析备忘录</h1>\n");
        if (source != null) {
            html.append("  <p>").append(source.getDocumentName()).append(" | ");
            html.append(source.getTotalSegments()).append(" 个片段</p>\n");
        }
        if (question != null) {
            html.append("  <p><strong>问题:</strong> ").append(escapeHtml(question)).append("</p>\n");
        }
        html.append("</div>\n");

        // 统计卡片
        List<MemoEntry> allMemos = new ArrayList<>();
        allMemos.addAll(longTermMemo);
        allMemos.addAll(shortTermMemory);

        html.append("<div class=\"section\">\n");
        html.append("  <h2>📊 统计概览</h2>\n");
        html.append("  <div class=\"stats\">\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-value\">").append(allMemos.size()).append("</div><div>总条目</div></div>\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-value\">").append(shortTermMemory.size()).append("</div><div>短期记忆</div></div>\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-value\">").append(longTermMemo.size()).append("</div><div>长期备忘录</div></div>\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-value\">").append(allMemos.stream().filter(MemoEntry::isIndependent).count()).append("</div><div>重要条目</div></div>\n");
        html.append("  </div>\n");
        html.append("</div>\n");

        // 内容
        html.append("<div class=\"section\">\n");
        html.append("  <h2>📝 备忘录内容</h2>\n");

        for (MemoEntry entry : allMemos) {
            String entryClass = entry.isIndependent() ? "entry important" : "entry";
            html.append("  <div class=\"").append(entryClass).append("\">\n");
            html.append("    <h4>第 ").append(entry.getSegmentIndex()).append(" 部分");
            if (entry.getTitle() != null) {
                html.append(": ").append(escapeHtml(entry.getTitle()));
            }
            if (entry.isIndependent()) {
                html.append(" ⭐");
            }
            html.append("</h4>\n");

            html.append("    <p>").append(escapeHtml(entry.getEffectiveContent())).append("</p>\n");

            if (entry.getKeywords() != null && !entry.getKeywords().isEmpty()) {
                html.append("    <div>\n");
                for (String keyword : entry.getKeywords()) {
                    html.append("      <span class=\"keyword\">").append(escapeHtml(keyword)).append("</span>\n");
                }
                html.append("    </div>\n");
            }

            html.append("  </div>\n");
        }

        html.append("</div>\n");

        html.append("<footer style=\"text-align:center; color:#999; margin-top:20px;\">\n");
        html.append("  生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        html.append("</footer>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 导出到文件
     */
    public void exportToFile(Path filePath, String content) throws IOException {
        Files.writeString(filePath, content);
        log.info("备忘录已导出到: {}", filePath);
    }

    // ==================== 辅助方法 ====================

    private String getTopicEmoji(String topic) {
        if (topic == null) return "📌";

        String lower = topic.toLowerCase();
        if (lower.contains("数据") || lower.contains("data")) return "📊";
        if (lower.contains("用户") || lower.contains("user")) return "👥";
        if (lower.contains("系统") || lower.contains("system")) return "⚙️";
        if (lower.contains("安全") || lower.contains("security")) return "🔒";
        if (lower.contains("性能") || lower.contains("performance")) return "⚡";
        if (lower.contains("设计") || lower.contains("design")) return "🎨";
        if (lower.contains("测试") || lower.contains("test")) return "🧪";
        if (lower.contains("总结") || lower.contains("summary")) return "📋";

        return "📌";
    }

    private String formatImportance(double importance) {
        if (importance >= 0.8) return "⭐⭐⭐ 高";
        if (importance >= 0.5) return "⭐⭐ 中";
        return "⭐ 低";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "<br>");
    }

    // ==================== 数据类 ====================

    @Data
    public static class ExportData {
        private DocumentSource source;
        private String question;
        private List<MemoEntry> shortTermMemory;
        private List<MemoEntry> longTermMemo;
        private List<MemoAggregator.TopicGroup> topicGroups;
        private Map<SegmentType, List<MemoEntry>> typeGroups;
        private Map<String, Object> stats;
        private String exportTime;
    }
}

