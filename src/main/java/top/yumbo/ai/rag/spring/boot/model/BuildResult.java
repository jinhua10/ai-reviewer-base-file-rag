package top.yumbo.ai.rag.spring.boot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库构建结果 / Knowledge Base Build Result
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Data
public class BuildResult {

    /**
     * 总文件数 / Total number of files
     */
    private int totalFiles = 0;

    /**
     * 成功处理的文件数 / Number of successfully processed files
     */
    private int successCount = 0;

    /**
     * 失败的文件数 / Number of failed files
     */
    private int failedCount = 0;

    /**
     * 创建的文档总数（包括分块后的）/ Total number of documents created (including chunked)
     */
    private int totalDocuments = 0;

    /**
     * 构建耗时（毫秒）/ Build time (milliseconds)
     */
    private long buildTimeMs = 0;

    /**
     * 错误信息（如果构建失败）/ Error message (if build failed)
     */
    private String error = null;

    /**
     * 失败的文件列表 / List of failed files
     */
    private List<String> failedFiles = new ArrayList<>();

    /**
     * 文件错误详情（文件名 -> 错误信息）
     */
    private Map<String, String> fileErrors = new HashMap<>();

    /**
     * 内存使用峰值（MB）
     */
    private long peakMemoryMB = 0;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return error == null && failedCount == 0;
    }

    /**
     * 添加失败文件
     */
    public void addFailedFile(String fileName, String errorMessage) {
        failedFiles.add(fileName);
        fileErrors.put(fileName, errorMessage);
        failedCount++;
    }
}

