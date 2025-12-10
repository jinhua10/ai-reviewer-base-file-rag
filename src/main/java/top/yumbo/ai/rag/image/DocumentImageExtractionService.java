package top.yumbo.ai.rag.image;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.image.analyzer.AIImageAnalyzer;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.image.extractor.impl.*;
import top.yumbo.ai.rag.impl.parser.image.SmartImageExtractor;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档图片提取管理服务（Document image extraction management service）
 * 负责协调各类文档的图片提取和 AI 分析（Responsible for coordinating image extraction and AI analysis for various document types）
 * <p>
 * 新增功能：使用 SmartImageExtractor 在索引阶段理解图片含义（OCR + Vision LLM）
 * New feature: Use SmartImageExtractor to understand image content during indexing (OCR + Vision LLM)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class DocumentImageExtractionService {

    private final List<DocumentImageExtractor> extractors;
    /**
     * -- GETTER --
     *  获取图片存储服务（Get image storage service）
     */
    @Getter
    private final ImageStorageService storageService;
    private final AIImageAnalyzer aiAnalyzer;
    private final boolean aiAnalysisEnabled;
    private final SmartImageExtractor smartImageExtractor;  // 新增：智能图片提取器

    public DocumentImageExtractionService(ImageStorageService storageService,
                                         AIImageAnalyzer aiAnalyzer,
                                         boolean aiAnalysisEnabled,
                                         SmartImageExtractor smartImageExtractor) {
        this.storageService = storageService;
        this.aiAnalyzer = aiAnalyzer;
        this.aiAnalysisEnabled = aiAnalysisEnabled;
        this.smartImageExtractor = smartImageExtractor;

        // 初始化所有提取器（Initialize all extractors）
        this.extractors = new ArrayList<>();

        // 新格式提取器 (Office 2007+)（New format extractors (Office 2007+)）
        this.extractors.add(new PdfImageExtractor());
        this.extractors.add(new WordImageExtractor());
        this.extractors.add(new PowerPointImageExtractor());
        this.extractors.add(new ExcelImageExtractor());

        // 老格式提取器 (Office 97-2003)（Legacy format extractors (Office 97-2003)）
        this.extractors.add(new WordLegacyImageExtractor());
        this.extractors.add(new PowerPointLegacyImageExtractor());
        this.extractors.add(new ExcelLegacyImageExtractor());

        log.info(I18N.get("log.image.service.init", extractors.size(), aiAnalysisEnabled));
        log.info(I18N.get("log.image.smart_extractor_strategy", smartImageExtractor.getActiveStrategy().getStrategyName()));
    }

    /**
     * 从文档中提取并保存图片（Extract and save images from document）
     *
     * @param documentFile 文档文件（Document file）
     * @param documentId 文档ID（用于存储）（Document ID (for storage)）
     * @return 保存的图片信息列表（List of saved image information）
     */
    public List<ImageInfo> extractAndSaveImages(File documentFile, String documentId) {
        String fileName = documentFile.getName();

        try (InputStream stream = new FileInputStream(documentFile)) {
            return extractAndSaveImages(stream, fileName, documentId);
        } catch (Exception e) {
            log.error(I18N.get("log.image.service.extract_failed", fileName), e);
            return new ArrayList<>();
        }
    }

    /**
     * 从文档中提取并保存图片（带位置信息）
     * Extract and save images from document (with position information)
     *
     * @param documentFile 文档文件（Document file）
     * @param documentId 文档ID（用于存储）（Document ID (for storage)）
     * @param originalContent 原始文本内容（用于计算图片位置）（Original text content (for calculating image position)）
     * @return 保存的图片信息列表（List of saved image information）
     */
    public List<ImageInfo> extractAndSaveImagesWithPosition(File documentFile, String documentId, String originalContent) {
        String fileName = documentFile.getName();

        try (InputStream stream = new FileInputStream(documentFile)) {
            return extractAndSaveImagesWithPosition(stream, fileName, documentId, originalContent);
        } catch (Exception e) {
            log.error(I18N.get("log.image.service.extract_failed", fileName), e);
            return new ArrayList<>();
        }
    }

    /**
     * 从文档流中提取并保存图片（Extract and save images from document stream）
     *
     * @param documentStream 文档输入流（Document input stream）
     * @param documentName 文档名称（Document name）
     * @param documentId 文档ID（Document ID）
     * @return 保存的图片信息列表（List of saved image information）
     */
    public List<ImageInfo> extractAndSaveImages(InputStream documentStream,
                                                String documentName,
                                                String documentId) {
        List<ImageInfo> savedImages = new ArrayList<>();

        try {
            log.info(I18N.get("log.image.service.start", documentName));

            // 1. 找到合适的提取器 (Find suitable extractor) (Find suitable extractor)
            DocumentImageExtractor extractor = findExtractor(documentName);
            if (extractor == null) {
                log.warn(I18N.get("log.image.service.no_extractor", documentName));
                return savedImages;
            }

            log.info(I18N.get("log.image.service.using_extractor", extractor.getName()));

            // 2. 提取图片 (Extract images) (Extract images)
            List<ExtractedImage> extractedImages = extractor.extractImages(documentStream, documentName);

            if (extractedImages.isEmpty()) {
                log.info(I18N.get("log.image.service.no_images", documentName));
                return savedImages;
            }

            log.info(I18N.get("log.image.service.extracted", extractedImages.size()));

            // 3. 使用 SmartImageExtractor 理解图片含义 (Use SmartImageExtractor to understand image content) (Use SmartImageExtractor to understand image content)
            // 这一步会执行 OCR 或 Vision LLM 分析，提取图片中的文字和语义 (This step performs OCR or Vision LLM analysis to extract text and semantics from images)
            for (ExtractedImage image : extractedImages) {
                try {
                    // 使用 SmartImageExtractor 提取图片内容 (Use SmartImageExtractor to extract image content)
                    ByteArrayInputStream imageStream = new ByteArrayInputStream(image.getData());
                    String imageContent = smartImageExtractor.extractContent(imageStream, image.getDisplayName());

                    // 将提取的内容设置为图片描述 (Set extracted content as image description)
                    if (imageContent != null && !imageContent.trim().isEmpty()) {
                        image.setAiDescription(imageContent);
                        log.debug(I18N.get("log.image.ai_analysis_complete", image.getDisplayName(), imageContent.length()));
                    }
                } catch (Exception e) {
                    log.warn(I18N.get("log.image.ai_analysis_failed", image.getDisplayName(), e.getMessage()));
                }
            }

            // 4. AI 分析图片（可选，如果还需要额外分析） (AI analyze images (optional, if additional analysis needed))
            if (aiAnalysisEnabled && aiAnalyzer != null) {
                extractedImages = aiAnalyzer.analyzeImages(extractedImages);
            } else {
                // 使用简单分析作为降级 (Use simple analysis as fallback)
                for (ExtractedImage image : extractedImages) {
                    if (aiAnalyzer != null) {
                        aiAnalyzer.simpleAnalyze(image);
                    }
                }
            }

            // 5. 保存图片到存储 (Save images to storage)
            for (ExtractedImage extracted : extractedImages) {
                try {
                    String originalName = extracted.getDisplayName();

                    ImageInfo savedImage = storageService.saveImage(
                            documentId,
                            extracted.getData(),
                            originalName
                    );

                    // 补充 AI 分析信息和位置信息 (Supplement AI analysis and position information) (Supplement AI analysis and position information)
                    savedImage.setDescription(extracted.getAiDescription());
                    savedImage.setOriginalFilename(extracted.getOriginalName());

                    // 设置位置信息（用于图片文本的原位置插入） (Set position information (for inserting image text at original position)) (Set position information (for inserting image text at original position))
                    savedImage.setPositionInDocument(extracted.getCharPositionInDocument());
                    savedImage.setContextBefore(extracted.getContextBefore());
                    savedImage.setContextAfter(extracted.getContextAfter());
                    savedImage.setExtractedText(extracted.getAiDescription());  // Vision LLM 提取的文本 (Text extracted by Vision LLM) (Text extracted by Vision LLM)

                    savedImages.add(savedImage);

                    log.info(I18N.get("log.image.service.saved", savedImage.getFilename(), extracted.getImageType(), extracted.getFileSize() / 1024));

                    if (extracted.getCharPositionInDocument() != null) {
                        log.debug(I18N.get("log.image.position_char_offset", extracted.getCharPositionInDocument()));
                    }

                } catch (Exception e) {
                    log.error(I18N.get("log.image.service.save_failed", extracted.getOriginalName()), e);
                }
            }

            log.info(I18N.get("log.image.service.success", savedImages.size(), documentName));

        } catch (Exception e) {
            log.error(I18N.get("log.image.service.failed", documentName), e);
        }

        return savedImages;
    }

    /**
     * 从文档流中提取并保存图片（带位置信息）
     * Extract and save images from document stream (with position information)
     */
    private List<ImageInfo> extractAndSaveImagesWithPosition(InputStream documentStream,
                                                             String documentName,
                                                             String documentId,
                                                             String originalContent) {
        List<ImageInfo> savedImages = new ArrayList<>();

        try {
            log.info(I18N.get("log.image.service.start", documentName));

            // 1. 找到合适的提取器 (Find suitable extractor)
            DocumentImageExtractor extractor = findExtractor(documentName);
            if (extractor == null) {
                log.warn(I18N.get("log.image.service.no_extractor", documentName));
                return savedImages;
            }

            log.info(I18N.get("log.image.service.using_extractor", extractor.getName()));

            // 2. 提取图片 (Extract images)
            List<ExtractedImage> extractedImages = extractor.extractImages(documentStream, documentName);

            if (extractedImages.isEmpty()) {
                log.info(I18N.get("log.image.service.no_images", documentName));
                return savedImages;
            }

            log.info(I18N.get("log.image.service.extracted", extractedImages.size()));

            // 2.5 计算图片在文档文本中的位置 (Calculate image positions in document text)
            calculateImagePositions(extractedImages, originalContent);

            // 3. 使用 SmartImageExtractor 理解图片含义 (Use SmartImageExtractor to understand image content)
            for (ExtractedImage image : extractedImages) {
                try {
                    // 使用 SmartImageExtractor 提取图片内容（传递上下文以提高准确度） (Use SmartImageExtractor to extract image content (pass context for better accuracy))
                    ByteArrayInputStream imageStream = new ByteArrayInputStream(image.getData());
                    String imageContent = smartImageExtractor.extractContent(imageStream, image.getDisplayName());

                    if (imageContent != null && !imageContent.trim().isEmpty()) {
                        image.setAiDescription(imageContent);
                        log.debug(I18N.get("log.image.ai_analysis_complete", image.getDisplayName(), imageContent.length()));
                    }
                } catch (Exception e) {
                    log.warn(I18N.get("log.image.ai_analysis_failed", image.getDisplayName(), e.getMessage()));
                }
            }

            // 4. 保存图片（复用原有逻辑） (Save images (reuse original logic))
            return saveExtractedImages(extractedImages, documentId, documentName);

        } catch (Exception e) {
            log.error(I18N.get("log.image.service.failed", documentName), e);
        }

        return savedImages;
    }

    /**
     * 计算图片在文档文本中的位置
     * Calculate image positions in document text
     */
    private void calculateImagePositions(List<ExtractedImage> images, String content) {
        if (content == null || content.isEmpty()) {
            log.debug(I18N.get("log.image.no_content_for_position"));
            return;
        }

        // 对于每个图片，根据其页码/位置信息估算在文本中的位置 (For each image, estimate its position in text based on page/position info)
        int totalLength = content.length();
        int imageCount = images.size();

        for (int i = 0; i < images.size(); i++) {
            ExtractedImage image = images.get(i);

            // 策略1：如果有页码信息，按页码比例估算位置 (Strategy 1: If page info available, estimate position by page ratio)
            if (image.getPosition() > 0) {
                // 假设图片均匀分布在文档中 (Assume images are evenly distributed in document)
                // 位置 = (图片页码 / 总图片数) * 文档总长度 (Position = (image page / total images) * total text length)
                int estimatedPosition = (int) ((double) (i + 1) / (imageCount + 1) * totalLength);
                image.setCharPositionInDocument(estimatedPosition);

                log.debug(I18N.get("log.image.estimated_position", image.getDisplayName(), estimatedPosition, i + 1, imageCount));
            }

            // 策略2：提取图片前后的上下文 (Strategy 2: Extract context before and after image)
            if (image.getCharPositionInDocument() != null) {
                int pos = image.getCharPositionInDocument();

                // 提取前100字符作为上下文 (Extract 100 characters before as context)
                int beforeStart = Math.max(0, pos - 100);
                int beforeEnd = pos;
                if (beforeEnd > beforeStart && beforeEnd <= content.length()) {
                    String contextBefore = content.substring(beforeStart, beforeEnd).trim();
                    image.setContextBefore(contextBefore);
                }

                // 提取后100字符作为上下文 (Extract 100 characters after as context)
                int afterStart = pos;
                int afterEnd = Math.min(content.length(), pos + 100);
                if (afterEnd > afterStart && afterStart < content.length()) {
                    String contextAfter = content.substring(afterStart, afterEnd).trim();
                    image.setContextAfter(contextAfter);
                }

                if (image.getContextBefore() != null || image.getContextAfter() != null) {
                    log.debug(I18N.get("log.image.context_extracted", 
                             image.getContextBefore() != null ? image.getContextBefore().length() : 0,
                             image.getContextAfter() != null ? image.getContextAfter().length() : 0));
                }
            }
        }
    }

    /**
     * 保存提取的图片（提取公共逻辑）
     * Save extracted images (extracted common logic)
     */
    private List<ImageInfo> saveExtractedImages(List<ExtractedImage> extractedImages,
                                               String documentId,
                                               String documentName) {
        List<ImageInfo> savedImages = new ArrayList<>();

        // AI 分析（如果启用） (AI analysis (if enabled))
        if (aiAnalysisEnabled && aiAnalyzer != null) {
            extractedImages = aiAnalyzer.analyzeImages(extractedImages);
        } else {
            for (ExtractedImage image : extractedImages) {
                if (aiAnalyzer != null) {
                    aiAnalyzer.simpleAnalyze(image);
                }
            }
        }

        // 保存图片到存储 (Save images to storage)
        for (ExtractedImage extracted : extractedImages) {
            try {
                String originalName = extracted.getDisplayName();

                ImageInfo savedImage = storageService.saveImage(
                        documentId,
                        extracted.getData(),
                        originalName
                );

                // 补充 AI 分析信息和位置信息 (Supplement AI analysis and position information)
                savedImage.setDescription(extracted.getAiDescription());
                savedImage.setOriginalFilename(extracted.getOriginalName());

                // 设置位置信息（用于图片文本的原位置插入） (Set position information (for inserting image text at original position))
                savedImage.setPositionInDocument(extracted.getCharPositionInDocument());
                savedImage.setContextBefore(extracted.getContextBefore());
                savedImage.setContextAfter(extracted.getContextAfter());
                savedImage.setExtractedText(extracted.getAiDescription());  // Vision LLM 提取的文本 (Text extracted by Vision LLM)

                savedImages.add(savedImage);

                log.info(I18N.get("log.image.service.saved", savedImage.getFilename(),
                         extracted.getImageType(), extracted.getFileSize() / 1024));

                if (extracted.getCharPositionInDocument() != null) {
                    log.debug(I18N.get("log.image.position_char_offset", extracted.getCharPositionInDocument()));
                }

            } catch (Exception e) {
                log.error(I18N.get("log.image.service.save_failed", extracted.getOriginalName()), e);
            }
        }

        log.info(I18N.get("log.image.service.success", savedImages.size(), documentName));
        return savedImages;
    }

    /**
     * 查找支持该文档类型的提取器（Find extractor that supports this document type）
     */
    private DocumentImageExtractor findExtractor(String fileName) {
        for (DocumentImageExtractor extractor : extractors) {
            if (extractor.supports(fileName)) {
                return extractor;
            }
        }
        return null;
    }

    /**
     * 判断是否支持该文档类型（Check if this document type is supported）
     */
    public boolean supportsDocument(String fileName) {
        return findExtractor(fileName) != null;
    }

    /**
     * 获取支持的文档格式列表（Get list of supported document formats）
     */
    public List<String> getSupportedFormats() {
        return List.of(
            ".pdf",
            ".docx", ".doc",      // Word
            ".pptx", ".ppt",      // PowerPoint
            ".xlsx", ".xls"       // Excel
        );
    }
}
