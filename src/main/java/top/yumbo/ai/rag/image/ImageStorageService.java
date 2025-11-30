package top.yumbo.ai.rag.image;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档图片提取和存储服务
 * 负责从文档中提取图片并保存到文件系统
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class ImageStorageService {

    private final String storageBasePath;
    private static final String IMAGE_DIR = "images";
    private static final List<String> SUPPORTED_FORMATS = List.of("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp");

    public ImageStorageService(String storageBasePath) {
        this.storageBasePath = storageBasePath;
        initializeStorage();
    }

    /**
     * 初始化存储目录
     */
    private void initializeStorage() {
        try {
            Path imagePath = Paths.get(storageBasePath, IMAGE_DIR);
            if (!Files.exists(imagePath)) {
                Files.createDirectories(imagePath);
                log.info(LogMessageProvider.getMessage("log.image.storage.created", imagePath.toString()));
            }
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.image.storage.init_failed"), e);
            throw new RuntimeException("Failed to initialize image storage", e);
        }
    }

    /**
     * 保存图片
     *
     * @param documentId 文档ID
     * @param imageData 图片数据
     * @param originalFilename 原始文件名
     * @return 图片存储信息
     */
    public ImageInfo saveImage(String documentId, byte[] imageData, String originalFilename) throws IOException {
        // 生成唯一文件名
        String extension = getFileExtension(originalFilename);
        String imageId = UUID.randomUUID().toString();
        String filename = String.format("%s_%s.%s", sanitizeFilename(documentId), imageId, extension);

        Path docImageDir = Paths.get(storageBasePath, IMAGE_DIR, sanitizeFilename(documentId));
        if (!Files.exists(docImageDir)) {
            Files.createDirectories(docImageDir);
        }

        Path imagePath = docImageDir.resolve(filename);
        Files.write(imagePath, imageData);

        log.info(LogMessageProvider.getMessage("log.image.saved", filename, documentId));

        return ImageInfo.builder()
                .imageId(imageId)
                .documentId(documentId)
                .filename(filename)
                .originalFilename(originalFilename)
                .filePath(imagePath.toString())
                .fileSize(imageData.length)
                .format(extension)
                .build();
    }

    /**
     * 保存图片从文件
     */
    public ImageInfo saveImageFromFile(String documentId, Path sourceImagePath) throws IOException {
        String originalFilename = sourceImagePath.getFileName().toString();
        byte[] imageData = Files.readAllBytes(sourceImagePath);
        return saveImage(documentId, imageData, originalFilename);
    }

    /**
     * 读取图片数据
     */
    public byte[] readImage(String documentId, String filename) throws IOException {
        Path imagePath = Paths.get(storageBasePath, IMAGE_DIR, sanitizeFilename(documentId), filename);

        if (!Files.exists(imagePath)) {
            throw new IOException("Image not found: " + filename);
        }

        return Files.readAllBytes(imagePath);
    }

    /**
     * 列出文档的所有图片
     */
    public List<ImageInfo> listImages(String documentId) throws IOException {
        Path docImageDir = Paths.get(storageBasePath, IMAGE_DIR, sanitizeFilename(documentId));

        if (!Files.exists(docImageDir)) {
            return List.of();
        }

        List<ImageInfo> images = new ArrayList<>();

        Files.list(docImageDir)
            .filter(Files::isRegularFile)
            .filter(p -> isSupportedImageFormat(p.getFileName().toString()))
            .forEach(imagePath -> {
                try {
                    String filename = imagePath.getFileName().toString();
                    long fileSize = Files.size(imagePath);

                    ImageInfo info = ImageInfo.builder()
                            .documentId(documentId)
                            .filename(filename)
                            .filePath(imagePath.toString())
                            .fileSize(fileSize)
                            .format(getFileExtension(filename))
                            .build();

                    images.add(info);
                } catch (IOException e) {
                    log.warn(LogMessageProvider.getMessage("log.image.read_info_failed", imagePath.toString()), e);
                }
            });

        return images;
    }

    /**
     * 删除文档的所有图片
     */
    public void deleteImages(String documentId) throws IOException {
        Path docImageDir = Paths.get(storageBasePath, IMAGE_DIR, sanitizeFilename(documentId));

        if (Files.exists(docImageDir)) {
            Files.walk(docImageDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn(LogMessageProvider.getMessage("log.image.delete_failed", path.toString()), e);
                    }
                });

            log.info(LogMessageProvider.getMessage("log.image.deleted_all", documentId));
        }
    }

    /**
     * 生成图片访问 URL
     */
    public String generateImageUrl(String documentId, String filename) {
        return String.format("/api/images/%s/%s", sanitizeFilename(documentId), filename);
    }

    /**
     * 将文档中的图片引用替换为实际 URL
     */
    public String replaceImageReferences(String content, String documentId, List<ImageInfo> images) {
        if (content == null || images == null || images.isEmpty()) {
            return content;
        }

        String result = content;

        for (ImageInfo image : images) {
            // 替换 Markdown 图片引用
            // ![alt](filename) -> ![alt](/api/images/docId/filename)
            String imageUrl = generateImageUrl(documentId, image.getFilename());

            // 尝试多种匹配模式
            result = result.replaceAll(
                "!\\[([^\\]]*)\\]\\(" + image.getOriginalFilename() + "\\)",
                "![$1](" + imageUrl + ")"
            );

            result = result.replaceAll(
                "!\\[([^\\]]*)\\]\\(" + image.getFilename() + "\\)",
                "![$1](" + imageUrl + ")"
            );
        }

        return result;
    }

    /**
     * 从 Markdown 内容中提取图片引用
     */
    public List<String> extractImageReferences(String markdownContent) {
        List<String> imageRefs = new ArrayList<>();

        if (markdownContent == null) {
            return imageRefs;
        }

        // 匹配 Markdown 图片语法: ![alt](url)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("!\\[[^\\]]*\\]\\(([^)]+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(markdownContent);

        while (matcher.find()) {
            String imageRef = matcher.group(1);
            imageRefs.add(imageRef);
        }

        return imageRefs;
    }

    /**
     * 判断是否为支持的图片格式
     */
    private boolean isSupportedImageFormat(String filename) {
        String extension = getFileExtension(filename);
        return SUPPORTED_FORMATS.contains(extension.toLowerCase());
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // 默认
        }

        int lastDot = filename.lastIndexOf('.');
        return filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 清理文件名
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }

        return filename
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .trim();
    }
}
