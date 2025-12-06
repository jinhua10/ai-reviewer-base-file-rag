package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.chunking.strategy.ChunkingStrategy;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.image.ImageInfo;
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
    private final top.yumbo.ai.rag.chunking.strategy.ChunkingStrategyFactory chunkingStrategyFactory;

    @Value("${knowledge.qa.chunking.strategy:ppl}")
    private String chunkingStrategy;

    public DocumentPreprocessingService(
            @Autowired(required = false) top.yumbo.ai.rag.ppl.config.PPLConfig pplConfig,
            @Autowired(required = false) PPLServiceFacade pplServiceFacade,
            top.yumbo.ai.rag.image.DocumentImageExtractionService imageExtractionService,
            top.yumbo.ai.rag.image.ImageStorageService imageStorageService,
            @Autowired(required = false) top.yumbo.ai.rag.chunking.strategy.ChunkingStrategyFactory chunkingStrategyFactory) {
        this.pplConfig = pplConfig;
        this.pplServiceFacade = pplServiceFacade;
        this.imageExtractionService = imageExtractionService;
        this.imageStorageService = imageStorageService;
        this.chunkingStrategyFactory = chunkingStrategyFactory;

        // 记录PPL服务状态（Log PPL service status）
        if (pplServiceFacade == null || pplConfig == null) {
            log.info(I18N.get("doc_preprocess.log.ppl_disabled"));
        } else {
            log.info(I18N.get("doc_preprocess.log.ppl_enabled"));
        }

        // 记录分块策略
        if (chunkingStrategyFactory != null) {
            log.info("📦 分块策略工厂已启用");
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

        // 1. 提取图片并进行 OCR/Vision LLM 处理
        if (imageExtractionService != null && imageExtractionService.supportsDocument(file.getName())) {
            try {
                log.info("🖼️ Starting image extraction for document: {}", file.getName());

                // 使用新方法：传递原始内容以便计算图片位置
                List<ImageInfo> images =
                    imageExtractionService.extractAndSaveImagesWithPosition(
                        file, file.getName(), originalContent);

                if (!images.isEmpty()) {
                    log.info("✅ Extracted {} images from {}", images.size(), file.getName());

                    // 2. 将图片文本插入到原始位置（而不是末尾）
                    String enhancedContent = insertImageTextAtOriginalPositions(
                        originalContent, images, file.getName());

                    log.info("✅ Image information inserted at original positions");
                    return enhancedContent;
                }
            } catch (Exception e) {
                log.warn("⚠️ Image extraction failed for {}: {}", file.getName(), e.getMessage());
            }
        }

        return originalContent;
    }

    /**
     * 将图片文本插入到原始位置
     * Insert image text at original positions
     *
     * @param originalContent 原始内容
     * @param images 图片列表
     * @param documentName 文档名称
     * @return 增强后的内容
     */
    private String insertImageTextAtOriginalPositions(
            String originalContent,
            List<top.yumbo.ai.rag.image.ImageInfo> images,
            String documentName) {

        // 1. 过滤出有位置信息和提取文本的图片
        List<top.yumbo.ai.rag.image.ImageInfo> validImages = images.stream()
            .filter(img -> img.getPositionInDocument() != null &&
                          img.getExtractedText() != null &&
                          !img.getExtractedText().trim().isEmpty())
            .toList();

        if (validImages.isEmpty()) {
            // 如果没有有效的图片位置信息，使用旧的方式（追加到末尾）
            log.debug("No valid image position info, appending to end");
            return originalContent + "\n\n" + buildImageTextContent(images, documentName);
        }

        // 2. 按位置倒序排序（避免插入时位置偏移）
        List<top.yumbo.ai.rag.image.ImageInfo> sortedImages = validImages.stream()
            .sorted((a, b) -> Integer.compare(
                b.getPositionInDocument(),
                a.getPositionInDocument()))
            .toList();

        // 3. 在原始位置插入图片文本
        StringBuilder enhancedContent = new StringBuilder(originalContent);

        for (top.yumbo.ai.rag.image.ImageInfo img : sortedImages) {
            // 构建图片文本标记（精简格式）
            String imageMarker = String.format(
                "\n\n[图片-%s：%s]\n\n",
                img.getFilename(),
                img.getExtractedText()
            );

            // 在原始位置插入（限制位置不超过当前长度）
            int insertPos = Math.min(
                img.getPositionInDocument(),
                enhancedContent.length());

            enhancedContent.insert(insertPos, imageMarker);

            log.debug("📍 Inserted image text at position {} for image: {}",
                     insertPos, img.getFilename());
        }

        return enhancedContent.toString();
    }

    /**
     * 使用智能策略对文档进行切分（支持 PPL/LLM/Auto）
     *
     * @param document 文档
     * @return 切分后的文档块列表
     */
    public List<Document> chunkDocumentWithPPL(Document document) {
        // 使用策略工厂进行分块
        if (chunkingStrategyFactory != null) {
            try {
                // 获取配置的策略
                ChunkingStrategy strategy =
                    chunkingStrategyFactory.getStrategy(chunkingStrategy);

                log.info("🔄 Starting chunking with strategy: {} for document: {}",
                         strategy.getStrategyName(), document.getTitle());
                long startTime = System.currentTimeMillis();

                // 获取分块配置
                ChunkConfig chunkConfig = getChunkConfig();

                // 执行分块
                List<DocumentChunk> chunks = strategy.chunk(
                    document.getContent(),
                    null,  // query 为 null，表示通用切分
                    chunkConfig
                );

                long chunkTime = System.currentTimeMillis() - startTime;
                log.info("✅ Chunking completed: {} chunks in {}ms using {}",
                         chunks.size(), chunkTime, strategy.getStrategyName());

                // 转换为 Document 列表
                return convertChunksToDocuments(chunks, document);

            } catch (Exception e) {
                log.warn("⚠️ Strategy-based chunking failed: {}, falling back to original document",
                         e.getMessage());
                return List.of(document);
            }
        }

        // 降级：使用传统 PPL 方式（兼容旧代码）
        return chunkWithLegacyPPL(document);
    }

    /**
     * 传统 PPL 分块方式（降级）
     */
    private List<Document> chunkWithLegacyPPL(Document document) {
        // 检查 PPL 服务是否可用
        if (pplConfig == null || pplServiceFacade == null) {
            log.debug("📦 PPL service not available, returning original document");
            return List.of(document);
        }

        // 检查是否启用 PPL Chunking
        ChunkConfig chunkConfig = pplConfig.getChunking();
        if (chunkConfig == null || (!chunkConfig.isEnableCoarseChunking() && chunkConfig.getPplThreshold() <= 0)) {
            return List.of(document);
        }

        try {
            log.info("🔄 Starting legacy PPL-based chunking for document: {}", document.getTitle());
            long startTime = System.currentTimeMillis();

            List<DocumentChunk> chunks = pplServiceFacade.chunk(
                document.getContent(),
                null
            );

            long chunkTime = System.currentTimeMillis() - startTime;
            log.info("✅ Legacy PPL chunking completed: {} chunks in {}ms", chunks.size(), chunkTime);

            return convertChunksToDocuments(chunks, document);

        } catch (PPLException e) {
            log.warn("⚠️ PPL chunking failed, using original document: {}", e.getMessage());
            return List.of(document);
        }
    }

    /**
     * 获取分块配置
     */
    private ChunkConfig getChunkConfig() {
        if (pplConfig != null && pplConfig.getChunking() != null) {
            return pplConfig.getChunking();
        }

        // 使用默认配置
        ChunkConfig config = new ChunkConfig();
        config.setMaxChunkSize(2500);
        config.setMinChunkSize(300);
        config.setOverlapSize(150);
        config.setPplThreshold(20.0);
        config.setEnableCoarseChunking(true);
        return config;
    }

    /**
     * 将 DocumentChunk 列表转换为 Document 列表
     */
    private List<Document> convertChunksToDocuments(List<DocumentChunk> chunks, Document originalDocument) {
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);

            Document chunkDoc = Document.builder()
                .title(originalDocument.getTitle() + " (块" + (i + 1) + "/" + chunks.size() + ")")
                .content(chunk.getContent())
                .metadata(originalDocument.getMetadata())
                .build();

            documents.add(chunkDoc);
        }

        return documents;
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

