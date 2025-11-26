package top.yumbo.ai.rag.image;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.image.analyzer.AIImageAnalyzer;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.image.extractor.impl.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档图片提取管理服务
 * 负责协调各类文档的图片提取和 AI 分析
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

        // 初始化所有提取器
        this.extractors = new ArrayList<>();
        this.extractors.add(new PdfImageExtractor());
        this.extractors.add(new WordImageExtractor());
        this.extractors.add(new PowerPointImageExtractor());
        this.extractors.add(new ExcelImageExtractor());

        log.info("DocumentImageExtractionService initialized with {} extractors, AI analysis: {}",
                extractors.size(), aiAnalysisEnabled);
    }

    /**
     * 从文档中提取并保存图片
     *
     * @param documentFile 文档文件
     * @param documentId 文档ID（用于存储）
     * @return 保存的图片信息列表
     */
    public List<ImageInfo> extractAndSaveImages(File documentFile, String documentId) {
        String fileName = documentFile.getName();

        try (InputStream stream = new FileInputStream(documentFile)) {
            return extractAndSaveImages(stream, fileName, documentId);
        } catch (Exception e) {
            log.error("Failed to extract images from file: {}", fileName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从文档流中提取并保存图片
     *
     * @param documentStream 文档输入流
     * @param documentName 文档名称
     * @param documentId 文档ID
     * @return 保存的图片信息列表
     */
    public List<ImageInfo> extractAndSaveImages(InputStream documentStream,
                                                String documentName,
                                                String documentId) {
        List<ImageInfo> savedImages = new ArrayList<>();

        try {
            log.info("🖼️ Starting image extraction from document: {}", documentName);

            // 1. 找到合适的提取器
            DocumentImageExtractor extractor = findExtractor(documentName);
            if (extractor == null) {
                log.warn("No extractor found for document: {}", documentName);
                return savedImages;
            }

            log.info("Using extractor: {}", extractor.getName());

            // 2. 提取图片
            List<ExtractedImage> extractedImages = extractor.extractImages(documentStream, documentName);

            if (extractedImages.isEmpty()) {
                log.info("No images found in document: {}", documentName);
                return savedImages;
            }

            log.info("Extracted {} images from document", extractedImages.size());

            // 3. AI 分析图片（可选）
            if (aiAnalysisEnabled && aiAnalyzer != null) {
                extractedImages = aiAnalyzer.analyzeImages(extractedImages);
            } else {
                // 使用简单分析作为降级
                for (ExtractedImage image : extractedImages) {
                    if (aiAnalyzer != null) {
                        aiAnalyzer.simpleAnalyze(image);
                    }
                }
            }

            // 4. 保存图片到存储
            for (ExtractedImage extracted : extractedImages) {
                try {
                    String originalName = extracted.getDisplayName();

                    ImageInfo savedImage = storageService.saveImage(
                            documentId,
                            extracted.getData(),
                            originalName
                    );

                    // 补充 AI 分析信息
                    savedImage.setDescription(extracted.getAiDescription());
                    savedImage.setOriginalFilename(extracted.getOriginalName());

                    savedImages.add(savedImage);

                    log.info("  ✅ Saved image: {} (type: {}, size: {}KB)",
                            savedImage.getFilename(),
                            extracted.getImageType(),
                            extracted.getFileSize() / 1024);

                } catch (Exception e) {
                    log.error("Failed to save image: {}", extracted.getOriginalName(), e);
                }
            }

            log.info("🎉 Successfully saved {} images from document: {}",
                    savedImages.size(), documentName);

        } catch (Exception e) {
            log.error("Failed to extract and save images from document: {}", documentName, e);
        }

        return savedImages;
    }

    /**
     * 查找支持该文档类型的提取器
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
     * 判断是否支持该文档类型
     */
    public boolean supportsDocument(String fileName) {
        return findExtractor(fileName) != null;
    }

    /**
     * 获取支持的文档格式列表
     */
    public List<String> getSupportedFormats() {
        return List.of(".pdf", ".docx", ".pptx", ".xlsx");
    }
}

