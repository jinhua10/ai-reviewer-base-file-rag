package top.yumbo.ai.rag.repository;

import top.yumbo.ai.rag.concept.ConceptHistory;
import top.yumbo.ai.rag.concept.ConceptVersion;

import java.util.List;
import java.util.Optional;

/**
 * 概念历史仓库接口 (Concept History Repository Interface)
 *
 * 提供概念版本历史的存储操作
 * (Provides storage operations for concept version history)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public interface ConceptHistoryRepository {

    /**
     * 保存概念历史 (Save concept history)
     *
     * @param history 概念历史 (Concept history)
     * @return 保存后的历史 (Saved history)
     */
    ConceptHistory save(ConceptHistory history);

    /**
     * 根据概念ID查找历史 (Find history by concept ID)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 概念历史（可能不存在） (Concept history, may not exist)
     */
    Optional<ConceptHistory> findByConceptId(String conceptId);

    /**
     * 查找所有历史记录 (Find all history records)
     *
     * @return 所有历史记录列表 (All history records list)
     */
    List<ConceptHistory> findAll();

    /**
     * 根据概念ID删除历史 (Delete history by concept ID)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 是否删除成功 (Whether deletion succeeded)
     */
    boolean deleteByConceptId(String conceptId);

    /**
     * 检查历史是否存在 (Check if history exists)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 是否存在 (Whether exists)
     */
    boolean existsByConceptId(String conceptId);

    /**
     * 获取历史记录总数 (Get total count)
     *
     * @return 历史记录总数 (Total count)
     */
    long count();

    /**
     * 清空所有历史记录 (Clear all history records)
     */
    void clear();

    /**
     * 获取指定概念的版本列表 (Get version list for specific concept)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 版本列表 (Version list)
     */
    List<ConceptVersion> findVersionsByConceptId(String conceptId);

    /**
     * 获取指定概念的特定版本 (Get specific version for concept)
     *
     * @param conceptId 概念ID (Concept ID)
     * @param versionNumber 版本号 (Version number)
     * @return 概念版本（可能不存在） (Concept version, may not exist)
     */
    Optional<ConceptVersion> findVersion(String conceptId, int versionNumber);
}

