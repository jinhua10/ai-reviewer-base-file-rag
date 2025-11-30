package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.image.ImageInfo;
import top.yumbo.ai.rag.image.ImageStorageService;

import java.util.List;

/**
 * 图片访问控制器 / Image Access Controller
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    /**
     * 获取图片 / Get image
     */
    @GetMapping("/{documentId}/{filename}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String documentId,
            @PathVariable String filename) {

        try {
            // 读取图片数据 / Read image data
            byte[] imageData = imageStorageService.readImage(documentId, filename);

            // 转换为资源 / Convert to resource
            ByteArrayResource resource = new ByteArrayResource(imageData);

            // 确定 MIME 类型 / Determine MIME type
            String extension = getFileExtension(filename);
            MediaType mediaType = getMediaType(extension);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(imageData.length)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000") // 缓存1年 / Cache for 1 year
                    .body(resource);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("image.log.get_failed", documentId, filename), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 列出文档的所有图片 / List all images of a document
     */
    @GetMapping("/list/{documentId}")
    public ResponseEntity<List<ImageInfo>> listImages(@PathVariable String documentId) {
        try {
            List<ImageInfo> images = imageStorageService.listImages(documentId);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("image.log.list_failed", documentId), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件扩展名确定 MIME 类型 / Determine MIME type by file extension
     */
    private MediaType getMediaType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "svg" -> MediaType.valueOf("image/svg+xml");
            case "webp" -> MediaType.valueOf("image/webp");
            case "bmp" -> MediaType.valueOf("image/bmp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /**
     * 获取文件扩展名 / Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }

        int lastDot = filename.lastIndexOf('.');
        return filename.substring(lastDot + 1);
    }
}

