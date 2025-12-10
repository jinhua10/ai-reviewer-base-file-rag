package top.yumbo.ai.rag.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import top.yumbo.ai.rag.concept.ConceptUnit;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于 JSON 文件的概念仓库实现 (JSON File-based Concept Repository Implementation)
 *
 * 使用 JSON 文件持久化概念数据
 * (Uses JSON files to persist concept data)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Repository
public class JsonConceptRepository implements ConceptRepository {

    @Value("${rag.storage.concepts.path:data/storage/concepts}")
    String storagePath;

    private static final String CONCEPTS_FILE = "concepts.json";

    /**
     * 内存缓存 (Memory cache)
     */
    private final Map<String, ConceptUnit> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // 确保存储目录存在 (Ensure storage directory exists)
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info(I18N.get("repository.concept.directory.created", storagePath));
            }

            // 加载数据 (Load data)
            loadFromFile();
            log.info(I18N.get("repository.concept.init.success", cache.size()));

        } catch (Exception e) {
            log.error(I18N.get("repository.concept.init.failed", e.getMessage()), e);
        }
    }

    @Override
    public ConceptUnit save(ConceptUnit concept) {
        if (concept == null || concept.getId() == null) {
            throw new IllegalArgumentException("Concept and ID cannot be null");
        }

        cache.put(concept.getId(), concept);
        persistToFile();

        log.debug(I18N.get("repository.concept.saved", concept.getId(), concept.getName()));
        return concept;
    }

    @Override
    public int saveAll(List<ConceptUnit> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ConceptUnit concept : concepts) {
            if (concept != null && concept.getId() != null) {
                cache.put(concept.getId(), concept);
                count++;
            }
        }

        persistToFile();
        log.info(I18N.get("repository.concept.batch.saved", count));
        return count;
    }

    @Override
    public Optional<ConceptUnit> findById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public List<ConceptUnit> findByName(String name) {
        return cache.values().stream()
                .filter(c -> c.getName() != null && c.getName().equals(name))
                .collect(Collectors.toList());
    }

    @Override
    public List<ConceptUnit> findByRoleId(String roleId) {
        return cache.values().stream()
                .filter(c -> c.getRoleId() != null && c.getRoleId().equals(roleId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ConceptUnit> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<ConceptUnit> findAllEnabled() {
        return cache.values().stream()
                .filter(ConceptUnit::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConceptUnit> findNeedsReview() {
        return cache.values().stream()
                .filter(ConceptUnit::isNeedsReview)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConceptUnit> findNeedsEvolution() {
        return cache.values().stream()
                .filter(ConceptUnit::needsEvolution)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(String id) {
        boolean existed = cache.remove(id) != null;
        if (existed) {
            persistToFile();
            log.info(I18N.get("repository.concept.deleted", id));
        }
        return existed;
    }

    @Override
    public boolean existsById(String id) {
        return cache.containsKey(id);
    }

    @Override
    public long count() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
        persistToFile();
        log.warn(I18N.get("repository.concept.cleared"));
    }

    // ==================== 私有方法 (Private Methods) ====================

    /**
     * 从文件加载数据 (Load data from file)
     */
    private void loadFromFile() {
        try {
            Path filePath = Paths.get(storagePath, CONCEPTS_FILE);

            if (!Files.exists(filePath)) {
                log.info(I18N.get("repository.concept.file.notfound", filePath));
                return;
            }

            String json = Files.readString(filePath);
            List<ConceptUnit> concepts = JSON.parseArray(json, ConceptUnit.class);

            if (concepts != null) {
                cache.clear();
                for (ConceptUnit concept : concepts) {
                    if (concept != null && concept.getId() != null) {
                        cache.put(concept.getId(), concept);
                    }
                }
                log.info(I18N.get("repository.concept.loaded", cache.size()));
            }

        } catch (IOException e) {
            log.error(I18N.get("repository.concept.load.failed", e.getMessage()), e);
        }
    }

    /**
     * 持久化到文件 (Persist to file)
     */
    private void persistToFile() {
        try {
            Path filePath = Paths.get(storagePath, CONCEPTS_FILE);

            // 确保目录存在 (Ensure directory exists)
            Files.createDirectories(filePath.getParent());

            // 转换为 JSON (Convert to JSON)
            List<ConceptUnit> concepts = new ArrayList<>(cache.values());
            String json = JSON.toJSONString(concepts, JSONWriter.Feature.PrettyFormat);

            // 写入文件 (Write to file)
            Files.writeString(filePath, json);

            log.debug(I18N.get("repository.concept.persisted", cache.size()));

        } catch (IOException e) {
            log.error(I18N.get("repository.concept.persist.failed", e.getMessage()), e);
        }
    }
}

