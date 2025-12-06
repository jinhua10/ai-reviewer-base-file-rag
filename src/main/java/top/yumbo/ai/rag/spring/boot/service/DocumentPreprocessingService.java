package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.ppl.PPLException;
import top.yumbo.ai.rag.ppl.PPLServiceFacade;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档智能预处理服务（Document intelligent preprocessing service）
 *
 * 功能：（Features:）
 * 1. 图片内容提取和OCR（Image content extraction and OCR）
 * 2. 基于 PPL 的智能切分（可选）（PPL-based intelligent chunking - optional）
 * 3. 内容增强和优化（Content enhancement and optimization）
 *
 * 注意：PPL 服务是可选的，当配置禁用时不影响基本功能
 * (Note: PPL service is optional, basic functionality is not affected when disabled)
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@Service
public class DocumentPreprocessingService {

    private final top.yumbo.ai.rag.ppl.config.PPLConfig pplConfig;
    private final PPLServiceFacade pplServiceFacade;
    private final top.yumbo.ai.rag.image.DocumentImageExtractionService imageExtractionService;
    private final top.yumbo.ai.rag.image.ImageStorageService imageStorageService;

    public DocumentPreprocessingService(
            @Autowired(required = false) top.yumbo.ai.rag.ppl.config.PPLConfig pplConfig,
            @Autowired(required = false) PPLServiceFacade pplServiceFacade,
            top.yumbo.ai.rag.image.DocumentImageExtractionService imageExtractionService,
            top.yumbo.ai.rag.image.ImageStorageService imageStorageService) {
        this.pplConfig = pplConfig;
        this.pplServiceFacade = pplServiceFacade;
        this.imageExtractionService = imageExtractionService;
        this.imageStorageService = imageStorageService;

        // 记录PPL服务状态（Log PPL service status）
        if (pplServiceFacade == null || pplConfig == null) {
            log.info(I18N.get("doc_preprocess.log.ppl_disabled"));
        } else {
            log.info(I18N.get("doc_preprocess.log.ppl_enabled"));
        }
    }

    /**
     * 预处理文档：提取图片并进行文本化
     *
     * @param file 文档文件
     * @param originalContent 原始文本内容
     * @return 增强后的内容（包含图片文本化信息）
     */
    public String preprocessDocument(File file, String originalContent) {
        if (originalContent == null || originalContent.trim().isEmpty()) {
            return originalContent;
        }

        StringBuilder enhancedContent = new StringBuilder(originalContent);

        // 1. 提取图片并进行 OCR/Vision LLM 处理
        if (imageExtractionService != null && imageExtractionService.supportsDocument(file.getName())) {
            try {
                log.info("🖼️ Starting image extraction for document: {}", file.getName());

                List<top.yumbo.ai.rag.image.ImageInfo> images =
                    imageExtractionService.extractAndSaveImages(file, file.getName());

                if (!images.isEmpty()) {
                    log.info("✅ Extracted {} images from {}", images.size(), file.getName());

                    // 构建图片信息文本
                    String imageText = buildImageTextContent(images, file.getName());

                    // 将图片信息添加到文档内容中
                    enhancedContent.append("\n\n").append(imageText);

                    log.info("✅ Image information added to document content ({} characters)",
                            imageText.length());
                }
            } catch (Exception e) {
                log.warn("⚠️ Image extraction failed for {}: {}", file.getName(), e.getMessage());
            }
        }

        return enhancedContent.toString();
    }

    /**
     * 使用 PPL 对文档进行智能切分
     *
     * @param document 文档
     * @return 切分后的文档块列表
     */
    public List<Document> chunkDocumentWithPPL(Document document) {
        // 检查 PPL 服务是否可用
        if (pplConfig == null || pplServiceFacade == null) {
            log.debug("📦 PPL service not available, returning original document");
            return List.of(document);
        }

        // 检查是否启用 PPL Chunking
        ChunkConfig chunkConfig = pplConfig.getChunking();
        if (chunkConfig == null || (!chunkConfig.isEnableCoarseChunking() && chunkConfig.getPplThreshold() <= 0)) {
            // PPL Chunking 未启用，返回原文档
            return List.of(document);
        }

        try {
            log.info("🔄 Starting PPL-based chunking for document: {}", document.getTitle());
            long startTime = System.currentTimeMillis();

            // 使用 PPL 服务进行智能切分
            // PPLServiceFacade.chunk 只需要 2 个参数: content, query
            // ChunkConfig 会从内部的 PPLConfig 获取
            List<DocumentChunk> chunks = pplServiceFacade.chunk(
                document.getContent(),
                null  // query 为 null，表示通用切分
            );

            long chunkTime = System.currentTimeMillis() - startTime;
            log.info("✅ PPL chunking completed: {} chunks in {}ms", chunks.size(), chunkTime);

            // 转换为 Document 列表
            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);

                Document chunkDoc = Document.builder()
                    .title(document.getTitle() + " (块" + (i + 1) + "/" + chunks.size() + ")")
                    .content(chunk.getContent())
                    .metadata(document.getMetadata())
                    .build();

                documents.add(chunkDoc);
            }

            return documents;

        } catch (PPLException e) {
            log.warn("⚠️ PPL chunking failed, using original document: {}", e.getMessage());
            return List.of(document);
        }
    }

    /**
     * 构建图片信息的文本表示
     */
    private String buildImageTextContent(List<top.yumbo.ai.rag.image.ImageInfo> images, String documentName) {
        StringBuilder imageText = new StringBuilder();

        imageText.append("\n\n========== 文档图片信息 ==========\n");
        imageText.append("来源文档: ").append(documentName).append("\n");
        imageText.append("图片数量: ").append(images.size()).append("\n\n");

        for (int i = 0; i < images.size(); i++) {
            top.yumbo.ai.rag.image.ImageInfo img = images.get(i);

            imageText.append("【图片 ").append(i + 1).append("】\n");
            imageText.append("文件名: ").append(img.getFilename()).append("\n");
            imageText.append("访问URL: ").append(img.getUrl()).append("\n");

            // 添加图片描述（OCR 或 Vision LLM 的结果）
            if (img.getDescription() != null && !img.getDescription().isEmpty()) {
                imageText.append("图片内容: ").append(img.getDescription()).append("\n");
            }

            // 添加图片格式
            if (img.getFormat() != null) {
                imageText.append("图片格式: ").append(img.getFormat()).append("\n");
            }

            // 添加图片大小
            if (img.getFileSize() > 0) {
                imageText.append("文件大小: ").append(img.getFileSize() / 1024).append(" KB\n");
            }

            // 添加原始文件名（如果来自 PPT/Word 等）
            if (img.getOriginalFilename() != null && !img.getOriginalFilename().equals(documentName)) {
                imageText.append("原始来源: ").append(img.getOriginalFilename()).append("\n");
            }

            imageText.append("\n");
        }

        imageText.append("========== 图片信息结束 ==========\n");

        return imageText.toString();
    }

    /**
     * 检查文档是否包含图片
     */
    public boolean hasImages(String documentName) {
        try {
            List<top.yumbo.ai.rag.image.ImageInfo> images =
                imageStorageService.listImages(documentName);
            return !images.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文档的图片信息
     */
    public List<top.yumbo.ai.rag.image.ImageInfo> getDocumentImages(String documentName) {
        try {
            return imageStorageService.listImages(documentName);
        } catch (Exception e) {
            log.warn("Failed to get images for document: {}", documentName, e);
            return List.of();
        }
    }
}

