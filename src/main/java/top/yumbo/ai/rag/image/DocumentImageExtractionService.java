package top.yumbo.ai.rag.image;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.image.analyzer.AIImageAnalyzer;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.image.extractor.impl.*;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档图片提取管理服务（Document image extraction management service）
 * 负责协调各类文档的图片提取和 AI 分析（Responsible for coordinating image extraction and AI analysis for various document types）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class DocumentImageExtractionService {

    private final List<DocumentImageExtractor> extractors;
    private final ImageStorageService storageService;
    private final AIImageAnalyzer aiAnalyzer;
    private final boolean aiAnalysisEnabled;

    public DocumentImageExtractionService(ImageStorageService storageService,
                                         AIImageAnalyzer aiAnalyzer,
                                         boolean aiAnalysisEnabled) {
        this.storageService = storageService;
        this.aiAnalyzer = aiAnalyzer;
        this.aiAnalysisEnabled = aiAnalysisEnabled;

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

        log.info(LogMessageProvider.getMessage("log.image.service.init", extractors.size(), aiAnalysisEnabled));
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
            log.error(LogMessageProvider.getMessage("log.image.service.extract_failed", fileName), e);
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
            log.info(LogMessageProvider.getMessage("log.image.service.start", documentName));

            // 1. 找到合适的提取器（Find suitable extractor）
            DocumentImageExtractor extractor = findExtractor(documentName);
            if (extractor == null) {
                log.warn(LogMessageProvider.getMessage("log.image.service.no_extractor", documentName));
                return savedImages;
            }

            log.info(LogMessageProvider.getMessage("log.image.service.using_extractor", extractor.getName()));

            // 2. 提取图片（Extract images）
            List<ExtractedImage> extractedImages = extractor.extractImages(documentStream, documentName);

            if (extractedImages.isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.image.service.no_images", documentName));
                return savedImages;
            }

            log.info(LogMessageProvider.getMessage("log.image.service.extracted", extractedImages.size()));

            // 3. AI 分析图片（可选）（AI analyze images (optional)）
            if (aiAnalysisEnabled && aiAnalyzer != null) {
                extractedImages = aiAnalyzer.analyzeImages(extractedImages);
            } else {
                // 使用简单分析作为降级（Use simple analysis as fallback）
                for (ExtractedImage image : extractedImages) {
                    if (aiAnalyzer != null) {
                        aiAnalyzer.simpleAnalyze(image);
                    }
                }
            }

            // 4. 保存图片到存储（Save images to storage）
            for (ExtractedImage extracted : extractedImages) {
                try {
                    String originalName = extracted.getDisplayName();

                    ImageInfo savedImage = storageService.saveImage(
                            documentId,
                            extracted.getData(),
                            originalName
                    );

                    // 补充 AI 分析信息（Supplement AI analysis information）
                    savedImage.setDescription(extracted.getAiDescription());
                    savedImage.setOriginalFilename(extracted.getOriginalName());

                    savedImages.add(savedImage);

                    log.info(LogMessageProvider.getMessage("log.image.service.saved", savedImage.getFilename(), extracted.getImageType(), extracted.getFileSize() / 1024));

                } catch (Exception e) {
                    log.error(LogMessageProvider.getMessage("log.image.service.save_failed", extracted.getOriginalName()), e);
                }
            }

            log.info(LogMessageProvider.getMessage("log.image.service.success", savedImages.size(), documentName));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.image.service.failed", documentName), e);
        }

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
