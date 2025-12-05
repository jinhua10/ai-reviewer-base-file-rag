package top.yumbo.ai.rag.spring.boot.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档片段 - 统一抽象不同文档类型的分析单位
 *
 * 支持的文档类型：
 * - PPT: 幻灯片
 * - PDF/Word: 页面、章节、段落
 * - Markdown: 标题块
 * - 代码: 文件、类、函数
 * - 通用: 文本块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSegment {
    /** 片段唯一标识 */
    private String id;

    /** 片段序号（从1开始） */
    private int index;

    /** 片段类型 */
    private SegmentType type;

    /** 片段标题/名称 */
    private String title;

    /** 文本内容 */
    private String textContent;

    /** 图片列表（Base64 或 URL） */
    @Builder.Default
    private List<String> images = new ArrayList<>();

    /** 表格数据（简化为字符串列表） */
    @Builder.Default
    private List<String> tables = new ArrayList<>();

    /** 代码块（如果是代码文件） */
    @Builder.Default
    private List<String> codeBlocks = new ArrayList<>();

    /** 元数据 */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /** 父片段ID（用于层级结构） */
    private String parentId;

    /** 子片段ID列表 */
    @Builder.Default
    private List<String> childIds = new ArrayList<>();

    /** 来源文档信息 */
    private DocumentSource source;

    /**
     * 获取片段的完整内容（用于分析）
     */
    public String getFullContent() {
        StringBuilder sb = new StringBuilder();

        if (title != null && !title.isEmpty()) {
            sb.append("**标题**: ").append(title).append("\n\n");
        }

        if (textContent != null && !textContent.isEmpty()) {
            sb.append("**内容**:\n").append(textContent).append("\n\n");
        }

        if (!images.isEmpty()) {
            sb.append("**图片数量**: ").append(images.size()).append(" 张\n\n");
        }

        if (!tables.isEmpty()) {
            sb.append("**表格数量**: ").append(tables.size()).append(" 个\n\n");
        }

        if (!codeBlocks.isEmpty()) {
            sb.append("**代码块**:\n");
            for (String code : codeBlocks) {
                sb.append("```\n").append(code).append("\n```\n\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * 获取片段的简要描述
     */
    public String getBriefDescription() {
        String desc = type.getDisplayName() + " " + index;
        if (title != null && !title.isEmpty()) {
            desc += ": " + title;
        }
        return desc;
    }

    /**
     * 判断片段是否有实质内容
     */
    public boolean hasContent() {
        return (textContent != null && !textContent.trim().isEmpty())
                || !images.isEmpty()
                || !tables.isEmpty()
                || !codeBlocks.isEmpty();
    }

    /**
     * 估算片段的内容长度（字符数）
     */
    public int estimateContentLength() {
        int length = 0;
        if (title != null) {
            length += title.length();
        }
        if (textContent != null) {
            length += textContent.length();
        }
        for (String table : tables) {
            length += table.length();
        }
        for (String code : codeBlocks) {
            length += code.length();
        }
        // 图片按平均描述长度估算
        length += images.size() * 50;
        return length;
    }

    /**
     * 创建 PPT 幻灯片片段
     */
    public static DocumentSegment createSlide(int slideNumber, String title, String content,
                                               List<String> images, DocumentSource source) {
        return DocumentSegment.builder()
                .id("slide-" + slideNumber)
                .index(slideNumber)
                .type(SegmentType.SLIDE)
                .title(title != null ? title : "幻灯片 " + slideNumber)
                .textContent(content)
                .images(images != null ? images : new ArrayList<>())
                .source(source)
                .build();
    }

    /**
     * 创建文本块片段
     */
    public static DocumentSegment createTextChunk(int chunkNumber, String content, DocumentSource source) {
        return DocumentSegment.builder()
                .id("chunk-" + chunkNumber)
                .index(chunkNumber)
                .type(SegmentType.TEXT_CHUNK)
                .title("文本块 " + chunkNumber)
                .textContent(content)
                .source(source)
                .build();
    }
}

