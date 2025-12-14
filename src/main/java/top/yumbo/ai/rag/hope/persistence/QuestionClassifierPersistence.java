package top.yumbo.ai.rag.hope.persistence;

import lombok.Data;
import top.yumbo.ai.rag.hope.QuestionClassifier.QuestionTypeConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 问题分类持久化服务接口
 * (Question Classifier Persistence Service Interface)
 *
 * <p>
 * 提供问题分类规则的持久化能力，支持：
 * (Provides persistence capabilities for question classification rules, supporting:)
 * <ul>
 *   <li>✅ 高性能读写 - 读写分离、缓存加速</li>
 *   <li>✅ 数据持久化 - 多种存储后端支持</li>
 *   <li>✅ 版本控制 - 规则变更历史追踪</li>
 *   <li>✅ 自动备份 - 定时备份、灾难恢复</li>
 *   <li>✅ 事务支持 - 保证数据一致性</li>
 * </ul>
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2.1.0
 */
public interface QuestionClassifierPersistence {

    /**
     * 保存问题类型配置
     * (Save question type configuration)
     *
     * @param config 问题类型配置
     * @return 是否成功
     */
    boolean saveQuestionType(QuestionTypeConfig config);

    /**
     * 批量保存问题类型配置
     * (Batch save question type configurations)
     *
     * @param configs 问题类型配置列表
     * @return 成功保存的数量
     */
    int saveQuestionTypes(List<QuestionTypeConfig> configs);

    /**
     * 获取问题类型配置
     * (Get question type configuration)
     *
     * @param typeId 类型ID
     * @return 问题类型配置
     */
    Optional<QuestionTypeConfig> getQuestionType(String typeId);

    /**
     * 获取所有问题类型配置
     * (Get all question type configurations)
     *
     * @return 问题类型配置列表
     */
    List<QuestionTypeConfig> getAllQuestionTypes();

    /**
     * 更新问题类型配置
     * (Update question type configuration)
     *
     * @param config 问题类型配置
     * @return 是否成功
     */
    boolean updateQuestionType(QuestionTypeConfig config);

    /**
     * 删除问题类型配置
     * (Delete question type configuration)
     *
     * @param typeId 类型ID
     * @return 是否成功
     */
    boolean deleteQuestionType(String typeId);

    /**
     * 保存关键词
     * (Save keywords)
     *
     * @param typeId   类型ID
     * @param keywords 关键词列表
     * @return 是否成功
     */
    boolean saveKeywords(String typeId, List<String> keywords);

    /**
     * 添加关键词
     * (Add keywords)
     *
     * @param typeId   类型ID
     * @param keywords 新增关键词
     * @return 是否成功
     */
    boolean addKeywords(String typeId, List<String> keywords);

    /**
     * 获取关键词
     * (Get keywords)
     *
     * @param typeId 类型ID
     * @return 关键词列表
     */
    List<String> getKeywords(String typeId);

    /**
     * 获取所有关键词
     * (Get all keywords)
     *
     * @return 关键词映射 (typeId -> keywords)
     */
    Map<String, List<String>> getAllKeywords();

    /**
     * 保存正则模式
     * (Save patterns)
     *
     * @param typeId   类型ID
     * @param patterns 模式列表
     * @return 是否成功
     */
    boolean savePatterns(String typeId, List<String> patterns);

    /**
     * 添加正则模式
     * (Add patterns)
     *
     * @param typeId   类型ID
     * @param patterns 新增模式
     * @return 是否成功
     */
    boolean addPatterns(String typeId, List<String> patterns);

    /**
     * 获取正则模式
     * (Get patterns)
     *
     * @param typeId 类型ID
     * @return 模式列表
     */
    List<String> getPatterns(String typeId);

    /**
     * 获取所有正则模式
     * (Get all patterns)
     *
     * @return 模式映射 (typeId -> patterns)
     */
    Map<String, List<String>> getAllPatterns();

    /**
     * 创建备份
     * (Create backup)
     *
     * @return 备份ID
     */
    String createBackup();

    /**
     * 从备份恢复
     * (Restore from backup)
     *
     * @param backupId 备份ID
     * @return 是否成功
     */
    boolean restoreFromBackup(String backupId);

    /**
     * 获取所有备份
     * (Get all backups)
     *
     * @return 备份ID列表
     */
    List<String> listBackups();

    /**
     * 获取配置版本
     * (Get configuration version)
     *
     * @return 版本号
     */
    String getVersion();

    /**
     * 保存配置版本
     * (Save configuration version)
     *
     * @param version 版本号
     * @return 是否成功
     */
    boolean saveVersion(String version);

    /**
     * 获取变更历史
     * (Get change history)
     *
     * @param limit 最大记录数
     * @return 变更记录列表
     */
    List<ChangeRecord> getChangeHistory(int limit);

    /**
     * 记录变更
     * (Record change)
     *
     * @param change 变更记录
     * @return 是否成功
     */
    boolean recordChange(ChangeRecord change);

    /**
     * 变更记录
     * (Change Record)
     */
    @Data
    class ChangeRecord {
        private String id;
        private String changeType;  // ADD, UPDATE, DELETE
        private String typeId;
        private String description;
        private long timestamp;
        private String userId;
    }
}

