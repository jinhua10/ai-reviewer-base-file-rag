package top.yumbo.ai.rag.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import top.yumbo.ai.rag.concept.ConceptHistory;
import top.yumbo.ai.rag.concept.ConceptVersion;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 JSON 文件的概念历史仓库实现 (JSON File-based Concept History Repository Implementation)
 *
 * 使用 JSON 文件持久化概念历史数据
 * (Uses JSON files to persist concept history data)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Repository
public class JsonConceptHistoryRepository implements ConceptHistoryRepository {

    @Value("${rag.storage.history.path:data/storage/history}")
    private String storagePath;

    private static final String HISTORY_FILE = "concept_history.json";

    /**
     * 内存缓存 (Memory cache)
     * Key: conceptId, Value: ConceptHistory
     */
    private final Map<String, ConceptHistory> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // 确保存储目录存在 (Ensure storage directory exists)
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info(I18N.get("repository.history.directory.created", storagePath));
            }

            // 加载数据 (Load data)
            loadFromFile();
            log.info(I18N.get("repository.history.init.success", cache.size()));

        } catch (Exception e) {
            log.error(I18N.get("repository.history.init.failed", e.getMessage()), e);
        }
    }

    @Override
    public ConceptHistory save(ConceptHistory history) {
        if (history == null || history.getConceptId() == null) {
            throw new IllegalArgumentException("History and concept ID cannot be null");
        }

        cache.put(history.getConceptId(), history);
        persistToFile();

        log.debug(I18N.get("repository.history.saved", history.getConceptId(), history.getVersionCount()));
        return history;
    }

    @Override
    public Optional<ConceptHistory> findByConceptId(String conceptId) {
        return Optional.ofNullable(cache.get(conceptId));
    }

    @Override
    public List<ConceptHistory> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public boolean deleteByConceptId(String conceptId) {
        boolean existed = cache.remove(conceptId) != null;
        if (existed) {
            persistToFile();
            log.info(I18N.get("repository.history.deleted", conceptId));
        }
        return existed;
    }

    @Override
    public boolean existsByConceptId(String conceptId) {
        return cache.containsKey(conceptId);
    }

    @Override
    public long count() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
        persistToFile();
        log.warn(I18N.get("repository.history.cleared"));
    }

    @Override
    public List<ConceptVersion> findVersionsByConceptId(String conceptId) {
        ConceptHistory history = cache.get(conceptId);
        if (history == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history.getVersions());
    }

    @Override
    public Optional<ConceptVersion> findVersion(String conceptId, int versionNumber) {
        ConceptHistory history = cache.get(conceptId);
        if (history == null) {
            return Optional.empty();
        }

        ConceptVersion version = history.getVersion(versionNumber);
        return Optional.ofNullable(version);
    }

    // ==================== 私有方法 (Private Methods) ====================

    /**
     * 从文件加载数据 (Load data from file)
     */
    private void loadFromFile() {
        try {
            Path filePath = Paths.get(storagePath, HISTORY_FILE);

            if (!Files.exists(filePath)) {
                log.info(I18N.get("repository.history.file.notfound", filePath));
                return;
            }

            String json = Files.readString(filePath);
            List<ConceptHistory> histories = JSON.parseArray(json, ConceptHistory.class);

            if (histories != null) {
                cache.clear();
                for (ConceptHistory history : histories) {
                    if (history != null && history.getConceptId() != null) {
                        cache.put(history.getConceptId(), history);
                    }
                }
                log.info(I18N.get("repository.history.loaded", cache.size()));
            }

        } catch (IOException e) {
            log.error(I18N.get("repository.history.load.failed", e.getMessage()), e);
        }
    }

    /**
     * 持久化到文件 (Persist to file)
     */
    private void persistToFile() {
        try {
            Path filePath = Paths.get(storagePath, HISTORY_FILE);

            // 确保目录存在 (Ensure directory exists)
            Files.createDirectories(filePath.getParent());

            // 转换为 JSON (Convert to JSON)
            List<ConceptHistory> histories = new ArrayList<>(cache.values());
            String json = JSON.toJSONString(histories, JSONWriter.Feature.PrettyFormat);

            // 写入文件 (Write to file)
            Files.writeString(filePath, json);

            log.debug(I18N.get("repository.history.persisted", cache.size()));

        } catch (IOException e) {
            log.error(I18N.get("repository.history.persist.failed", e.getMessage()), e);
        }
    }
}

