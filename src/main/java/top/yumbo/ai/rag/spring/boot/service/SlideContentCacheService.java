package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

/**
 * PPT 幻灯片内容缓存服务
 *
 * 功能：
 * 1. 缓存每张幻灯片经过 Vision LLM 处理后的文本内容
 * 2. 基于幻灯片内容哈希判断是否需要重新处理
 * 3. 支持幻灯片级别的增量更新
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
@Slf4j
@Service
public class SlideContentCacheService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String cacheDir;
    private Map<String, PPTCache> cacheIndex;

    /**
     * 初始化缓存服务
     */
    public void initialize(String storagePath) {
        this.cacheDir = storagePath + File.separator + ".slide_cache";
        Path cacheDirPath = Paths.get(cacheDir);

        try {
            if (!Files.exists(cacheDirPath)) {
                Files.createDirectories(cacheDirPath);
                log.info("创建幻灯片缓存目录: {}", cacheDir);
            }

            loadCacheIndex();

        } catch (IOException e) {
            log.error("初始化幻灯片缓存失败", e);
            this.cacheIndex = new HashMap<>();
        }
    }

    /**
     * 获取 PPT 文件的缓存
     */
    public PPTCache getPPTCache(String pptFilePath) {
        if (cacheIndex == null) {
            return null;
        }
        return cacheIndex.get(pptFilePath);
    }

    /**
     * 保存 PPT 缓存
     */
    public void savePPTCache(String pptFilePath, PPTCache cache) {
        if (cacheIndex == null) {
            cacheIndex = new HashMap<>();
        }

        cacheIndex.put(pptFilePath, cache);

        // 立即持久化
        try {
            saveCacheIndex();
        } catch (IOException e) {
            log.error("保存幻灯片缓存失败", e);
        }
    }

    /**
     * 获取幻灯片缓存内容
     */
    public SlideCache getSlideCache(String pptFilePath, int slideNumber) {
        PPTCache pptCache = getPPTCache(pptFilePath);
        if (pptCache == null) {
            return null;
        }
        return pptCache.getSlides().get(slideNumber);
    }

    /**
     * 检查幻灯片是否需要更新
     *
     * @param slideHash 幻灯片内容哈希
     * @param cachedSlide 缓存的幻灯片
     * @return true 需要更新, false 使用缓存
     */
    public boolean needsUpdate(String slideHash, SlideCache cachedSlide) {
        if (cachedSlide == null) {
            return true; // 没有缓存，需要处理
        }

        // 比较哈希值
        return !slideHash.equals(cachedSlide.getContentHash());
    }

    /**
     * 计算幻灯片内容哈希
     * 基于：文本内容 + 图片数据
     */
    public String calculateSlideHash(String slideText, List<byte[]> imageData) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // 加入文本
            if (slideText != null && !slideText.isEmpty()) {
                md.update(slideText.getBytes("UTF-8"));
            }

            // 加入所有图片数据
            if (imageData != null) {
                for (byte[] data : imageData) {
                    md.update(data);
                }
            }

            byte[] hashBytes = md.digest();

            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("计算幻灯片哈希失败", e);
            return UUID.randomUUID().toString(); // 失败时返回随机值，强制重新处理
        }
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        cacheIndex = new HashMap<>();
        try {
            saveCacheIndex();
        } catch (IOException e) {
            log.error("清空缓存失败", e);
        }
    }

    /**
     * 删除指定 PPT 的缓存
     */
    public void removePPTCache(String pptFilePath) {
        if (cacheIndex != null) {
            cacheIndex.remove(pptFilePath);
            try {
                saveCacheIndex();
            } catch (IOException e) {
                log.error("删除 PPT 缓存失败", e);
            }
        }
    }

    /**
     * 加载缓存索引
     */
    private void loadCacheIndex() throws IOException {
        Path indexPath = Paths.get(cacheDir, "cache_index.json");

        if (!Files.exists(indexPath)) {
            this.cacheIndex = new HashMap<>();
            return;
        }

        try {
            CacheIndex index = objectMapper.readValue(indexPath.toFile(), CacheIndex.class);
            this.cacheIndex = index.getPptCaches();
            log.info("加载幻灯片缓存索引: {} 个 PPT 文件", cacheIndex.size());
        } catch (Exception e) {
            log.warn("加载缓存索引失败，将创建新索引", e);
            this.cacheIndex = new HashMap<>();
        }
    }

    /**
     * 保存缓存索引
     */
    private void saveCacheIndex() throws IOException {
        Path indexPath = Paths.get(cacheDir, "cache_index.json");

        CacheIndex index = new CacheIndex();
        index.setPptCaches(cacheIndex);
        index.setLastUpdated(System.currentTimeMillis());

        objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValue(indexPath.toFile(), index);

        log.debug("保存幻灯片缓存索引: {} 个 PPT 文件", cacheIndex.size());
    }

    /**
     * 缓存索引
     */
    @Data
    public static class CacheIndex {
        private Map<String, PPTCache> pptCaches = new HashMap<>();
        private long lastUpdated;
    }

    /**
     * PPT 文件缓存
     */
    @Data
    public static class PPTCache {
        private String filePath;
        private long fileLastModified;
        private long fileSize;
        private int totalSlides;
        private Map<Integer, SlideCache> slides = new HashMap<>(); // slideNumber -> SlideCache
        private long cacheTime;
    }

    /**
     * 单张幻灯片缓存
     */
    @Data
    public static class SlideCache {
        private int slideNumber;
        private String contentHash; // 幻灯片内容哈希（文本+图片）
        private String slideText;   // 幻灯片文本内容
        private int imageCount;     // 图片数量
        private String visionLLMResult; // Vision LLM 处理结果
        private long processTime;   // 处理时间戳
    }
}

