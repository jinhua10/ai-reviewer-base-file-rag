package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageService;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文档块下载控制器 / Chunk Download Controller
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
@RestController
@RequestMapping("/api/chunks")
@CrossOrigin(origins = "*")
public class ChunkDownloadController {

    private final ChunkStorageService chunkStorageService;

    public ChunkDownloadController(ChunkStorageService chunkStorageService) {
        this.chunkStorageService = chunkStorageService;
    }

    /**
     * 列出文档的所有切分块 / List all chunks of a document
     */
    @GetMapping("/list/{documentId}")
    public ResponseEntity<List<ChunkStorageInfo>> listChunks(@PathVariable String documentId) {
        try {
            List<ChunkStorageInfo> chunks = chunkStorageService.listChunks(documentId);
            return ResponseEntity.ok(chunks);
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("chunk_download.log.list_failed", documentId), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 下载单个文档块 / Download a single chunk
     */
    @GetMapping("/download/{documentId}/{chunkId}")
    public ResponseEntity<Resource> downloadChunk(
            @PathVariable String documentId,
            @PathVariable String chunkId) {

        try {
            // 读取块内容 / Read chunk content
            String content = chunkStorageService.readChunkContent(chunkId, documentId);

            // 转换为资源 / Convert to resource
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(data);

            // 设置响应头 / Set response headers
            String filename = chunkId + ".md";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(data.length)
                    .body(resource);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("chunk_download.log.download_failed", documentId, chunkId), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取块内容（不下载，直接显示）/ Get chunk content (display without download)
     */
    @GetMapping("/content/{documentId}/{chunkId}")
    public ResponseEntity<String> getChunkContent(
            @PathVariable String documentId,
            @PathVariable String chunkId) {

        try {
            String content = chunkStorageService.readChunkContent(chunkId, documentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(content);
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("chunk_download.log.content_failed", documentId, chunkId), e);
            return ResponseEntity.notFound().build();
        }
    }
}

