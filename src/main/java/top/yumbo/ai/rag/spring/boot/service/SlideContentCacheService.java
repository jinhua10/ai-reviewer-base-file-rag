package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

/**
 * PPT 幻灯片内容缓存服务（PPT slide content cache service）
 *
 * 功能：（Features:）
 * 1. 缓存每张幻灯片经过 Vision LLM 处理后的文本内容（Cache slide content after Vision LLM processing）
 * 2. 基于幻灯片内容哈希判断是否需要重新处理（Check if reprocessing is needed based on content hash）
 * 3. 支持幻灯片级别的增量更新（Support incremental updates at slide level）
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
     * 初始化缓存服务（Initialize cache service）
     */
    public void initialize(String storagePath) {
        this.cacheDir = storagePath + File.separator + ".slide_cache";
        Path cacheDirPath = Paths.get(cacheDir);

        try {
            if (!Files.exists(cacheDirPath)) {
                Files.createDirectories(cacheDirPath);
                log.info(LogMessageProvider.getMessage("slide_cache.log.create_cache_dir", cacheDir));
            }

            loadCacheIndex();

        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("slide_cache.log.init_failed"), e);
            this.cacheIndex = new HashMap<>();
        }
    }

    /**
     * 获取 PPT 文件的缓存（Get PPT file cache）
     */
    public PPTCache getPPTCache(String pptFilePath) {
        if (cacheIndex == null) {
            return null;
        }
        return cacheIndex.get(pptFilePath);
    }

    /**
     * 保存 PPT 缓存（Save PPT cache）
     */
    public void savePPTCache(String pptFilePath, PPTCache cache) {
        if (cacheIndex == null) {
            cacheIndex = new HashMap<>();
        }

        cacheIndex.put(pptFilePath, cache);

        // 立即持久化（Persist immediately）
        try {
            saveCacheIndex();
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("slide_cache.log.save_failed"), e);
        }
    }

    /**
     * 获取幻灯片缓存内容（Get slide cache content）
     */
    public SlideCache getSlideCache(String pptFilePath, int slideNumber) {
        PPTCache pptCache = getPPTCache(pptFilePath);
        if (pptCache == null) {
            return null;
        }
        return pptCache.getSlides().get(slideNumber);
    }

    /**
     * 检查幻灯片是否需要更新（Check if slide needs update）
     *
     * @param slideHash 幻灯片内容哈希（Slide content hash）
     * @param cachedSlide 缓存的幻灯片（Cached slide）
     * @return true 需要更新, false 使用缓存（true needs update, false use cache）
     */
    public boolean needsUpdate(String slideHash, SlideCache cachedSlide) {
        if (cachedSlide == null) {
            return true; // 没有缓存，需要处理（No cache, need processing）
        }

        // 比较哈希值（Compare hash values）
        return !slideHash.equals(cachedSlide.getContentHash());
    }

    /**
     * 计算幻灯片内容哈希（Calculate slide content hash）
     * 基于：文本内容 + 图片数据（Based on: text content + image data）
     */
    public String calculateSlideHash(String slideText, List<byte[]> imageData) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // 加入文本（Add text）
            if (slideText != null && !slideText.isEmpty()) {
                md.update(slideText.getBytes("UTF-8"));
            }

            // 加入所有图片数据（Add all image data）
            if (imageData != null) {
                for (byte[] data : imageData) {
                    md.update(data);
                }
            }

            byte[] hashBytes = md.digest();

            // 转换为十六进制字符串（Convert to hex string）
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("slide_cache.log.hash_failed"), e);
            return UUID.randomUUID().toString(); // 失败时返回随机值，强制重新处理（Return random value on failure, force reprocessing）
        }
    }

    /**
     * 清空缓存（Clear cache）
     */
    public void clearCache() {
        cacheIndex = new HashMap<>();
        try {
            saveCacheIndex();
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("slide_cache.log.clear_failed"), e);
        }
    }

    /**
     * 删除指定 PPT 的缓存（Remove specific PPT cache）
     */
    public void removePPTCache(String pptFilePath) {
        if (cacheIndex != null) {
            cacheIndex.remove(pptFilePath);
            try {
                saveCacheIndex();
            } catch (IOException e) {
                log.error(LogMessageProvider.getMessage("slide_cache.log.delete_ppt_cache_failed"), e);
            }
        }
    }

    /**
     * 加载缓存索引（Load cache index）
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
            log.info(LogMessageProvider.getMessage("slide_cache.log.load_cache_index", cacheIndex.size()));
        } catch (Exception e) {
            log.warn(LogMessageProvider.getMessage("slide_cache.log.load_cache_index_failed"), e);
            this.cacheIndex = new HashMap<>();
        }
    }

    /**
     * 保存缓存索引（Save cache index）
     */
    private void saveCacheIndex() throws IOException {
        Path indexPath = Paths.get(cacheDir, "cache_index.json");

        CacheIndex index = new CacheIndex();
        index.setPptCaches(cacheIndex);
        index.setLastUpdated(System.currentTimeMillis());

        objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValue(indexPath.toFile(), index);

        log.debug(LogMessageProvider.getMessage("slide_cache.log.save_cache_index", cacheIndex.size()));
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

