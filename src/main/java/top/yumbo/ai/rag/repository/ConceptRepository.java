package top.yumbo.ai.rag.repository;

import top.yumbo.ai.rag.concept.ConceptUnit;

import java.util.List;
import java.util.Optional;

/**
 * 概念仓库接口 (Concept Repository Interface)
 *
 * 提供概念单元的 CRUD 操作
 * (Provides CRUD operations for concept units)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public interface ConceptRepository {

    /**
     * 保存概念 (Save concept)
     *
     * @param concept 概念单元 (Concept unit)
     * @return 保存后的概念 (Saved concept)
     */
    ConceptUnit save(ConceptUnit concept);

    /**
     * 批量保存概念 (Batch save concepts)
     *
     * @param concepts 概念列表 (Concept list)
     * @return 保存的数量 (Number of saved concepts)
     */
    int saveAll(List<ConceptUnit> concepts);

    /**
     * 根据ID查找概念 (Find concept by ID)
     *
     * @param id 概念ID (Concept ID)
     * @return 概念（可能不存在） (Concept, may not exist)
     */
    Optional<ConceptUnit> findById(String id);

    /**
     * 根据名称查找概念 (Find concepts by name)
     *
     * @param name 概念名称 (Concept name)
     * @return 概念列表 (Concept list)
     */
    List<ConceptUnit> findByName(String name);

    /**
     * 根据角色ID查找概念 (Find concepts by role ID)
     *
     * @param roleId 角色ID (Role ID)
     * @return 概念列表 (Concept list)
     */
    List<ConceptUnit> findByRoleId(String roleId);

    /**
     * 查找所有概念 (Find all concepts)
     *
     * @return 所有概念列表 (All concepts list)
     */
    List<ConceptUnit> findAll();

    /**
     * 查找启用的概念 (Find enabled concepts)
     *
     * @return 启用的概念列表 (Enabled concepts list)
     */
    List<ConceptUnit> findAllEnabled();

    /**
     * 查找需要审核的概念 (Find concepts needing review)
     *
     * @return 需要审核的概念列表 (Concepts needing review)
     */
    List<ConceptUnit> findNeedsReview();

    /**
     * 查找需要演化的概念 (Find concepts needing evolution)
     *
     * @return 需要演化的概念列表 (Concepts needing evolution)
     */
    List<ConceptUnit> findNeedsEvolution();

    /**
     * 删除概念 (Delete concept)
     *
     * @param id 概念ID (Concept ID)
     * @return 是否删除成功 (Whether deletion succeeded)
     */
    boolean deleteById(String id);

    /**
     * 检查概念是否存在 (Check if concept exists)
     *
     * @param id 概念ID (Concept ID)
     * @return 是否存在 (Whether exists)
     */
    boolean existsById(String id);

    /**
     * 获取概念总数 (Get total count)
     *
     * @return 概念总数 (Total count)
     */
    long count();

    /**
     * 清空所有概念 (Clear all concepts)
     */
    void clear();
}

