# RAG 2.0 后端待办任务清单
# RAG 2.0 Backend TODO Plan

> **创建日期**: 2025-12-12  
> **文档类型**: 任务清单（Task List）  
> **状态**: 📋 规划中  
> **优先级**: 🔥 高优先级

---

## 📋 任务概述

基于前端已完成的功能组件，后端需要实现对应的 API 接口和服务层逻辑。

### 当前状态
- ✅ **前端完成度**: Phase 7-9 已完成（100%）
  - 基础架构、核心功能界面、扩展功能界面
  - 总计 62 个 JSX 组件、45 个 CSS 文件
  
- ⚠️ **后端完成度**: 约 40%
  - ✅ 基础 RAG 功能（文档索引、搜索）
  - ✅ 文档管理基础 API
  - ✅ 测试和监控 API
  - ❌ 愿望单系统（0%）
  - ❌ AI 服务扩展（0%）
  - ❌ 个人中心（0%）
  - ❌ 系统管理（部分）
  - ❌ 协作网络（0%）

---

## 🎯 任务优先级分级

### P0 - 核心功能（必须完成）
**目标**: 让系统能跑起来，核心流程可用

1. ✅ 文档管理基础 API（已完成）
2. ✅ 问答基础 API（已完成）
3. ⏳ 角色管理 API（部分完成）
4. ⏳ 反馈系统 API（部分完成）

### P1 - 扩展功能（应该完成）
**目标**: 提供完整的功能体验

1. ❌ 愿望单系统 API
2. ❌ AI 服务扩展 API
3. ❌ 个人中心 API
4. ❌ 协作网络 API

### P2 - 系统管理（可以延后）
**目标**: 提供运维和管理能力

1. ⏳ 系统配置 API（部分完成）
2. ❌ 日志查看 API
3. ❌ 性能监控 API（已有基础，需完善）

---

## 📦 详细任务列表

### Task Group 1: 愿望单系统 🎯 **P1 - 高优先级**

#### 背景
前端已实现完整的愿望单界面（Phase 9.1），包括：
- WishList.jsx - 愿望单列表
- WishCard.jsx - 愿望卡片
- WishSubmit.jsx - 提交愿望
- WishVote.jsx - 投票组件
- WishComments.jsx - 评论系统
- WishRanking.jsx - 排行榜

#### 需要实现的后端接口

##### 1.1 WishController - 愿望单控制器
```java
@RestController
@RequestMapping("/api/wishes")
public class WishController {
    
    // GET /api/wishes - 获取愿望列表
    // 支持参数: status, category, sortBy, keyword
    @GetMapping
    public ResponseEntity<PageResult<WishDTO>> getWishes(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    );
    
    // GET /api/wishes/{id} - 获取愿望详情
    @GetMapping("/{id}")
    public ResponseEntity<WishDetailDTO> getWishDetail(@PathVariable Long id);
    
    // POST /api/wishes - 提交新愿望
    @PostMapping
    public ResponseEntity<WishDTO> submitWish(@RequestBody WishSubmitRequest request);
    
    // POST /api/wishes/{id}/vote - 投票
    @PostMapping("/{id}/vote")
    public ResponseEntity<VoteResult> voteWish(
        @PathVariable Long id,
        @RequestBody VoteRequest request
    );
    
    // GET /api/wishes/{id}/comments - 获取评论列表
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long id);
    
    // POST /api/wishes/{id}/comments - 添加评论
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDTO> addComment(
        @PathVariable Long id,
        @RequestBody CommentRequest request
    );
    
    // POST /api/comments/{id}/like - 点赞评论
    @PostMapping("/comments/{id}/like")
    public ResponseEntity<Void> likeComment(@PathVariable Long id);
    
    // GET /api/wishes/ranking - 获取排行榜
    @GetMapping("/ranking")
    public ResponseEntity<List<WishDTO>> getRanking(
        @RequestParam(defaultValue = "10") int limit
    );
}
```

##### 1.2 数据模型
```java
// 实体类
@Entity
@Table(name = "wishes")
@Data
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;           // 标题
    private String description;     // 描述
    private String category;        // 分类
    private String status;          // 状态 (pending/accepted/rejected/completed)
    private Long submitUserId;      // 提交用户
    private Integer voteCount;      // 投票数
    private Integer upVotes;        // 赞成票
    private Integer downVotes;      // 反对票
    private Integer commentCount;   // 评论数
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Entity
@Table(name = "wish_votes")
@Data
public class WishVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long wishId;
    private Long userId;
    private String voteType;  // up/down
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "wish_comments")
@Data
public class WishComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long wishId;
    private Long userId;
    private Long parentId;    // 父评论ID（回复）
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
}
```

##### 1.3 服务层
```java
@Service
public class WishService {
    // 获取愿望列表（支持筛选、排序）
    Page<WishDTO> getWishes(WishQueryParams params);
    
    // 获取愿望详情
    WishDetailDTO getWishDetail(Long id);
    
    // 提交新愿望
    WishDTO submitWish(WishSubmitRequest request);
    
    // 投票（防止重复投票）
    VoteResult voteWish(Long wishId, Long userId, String voteType);
    
    // 获取评论列表（含嵌套回复）
    List<CommentDTO> getComments(Long wishId);
    
    // 添加评论
    CommentDTO addComment(Long wishId, CommentRequest request);
    
    // 点赞评论
    void likeComment(Long commentId, Long userId);
    
    // 获取排行榜（按投票数排序）
    List<WishDTO> getRanking(int limit);
}
```

##### 1.4 国际化
```yaml
# zh/zh-wish.yml
wish:
  submit:
    success: "✅ 愿望提交成功"
    failed: "❌ 愿望提交失败: {0}"
  vote:
    success: "✅ 投票成功"
    failed: "❌ 投票失败: {0}"
    duplicate: "⚠️ 您已经投过票了"
  comment:
    add_success: "✅ 评论添加成功"
    add_failed: "❌ 评论添加失败: {0}"
  ranking:
    load_success: "✅ 排行榜加载成功"

# en/en-wish.yml
wish:
  submit:
    success: "✅ Wish submitted successfully"
    failed: "❌ Failed to submit wish: {0}"
  vote:
    success: "✅ Vote submitted successfully"
    failed: "❌ Failed to vote: {0}"
    duplicate: "⚠️ You have already voted"
  comment:
    add_success: "✅ Comment added successfully"
    add_failed: "❌ Failed to add comment: {0}"
  ranking:
    load_success: "✅ Ranking loaded successfully"
```

##### 1.5 预计工作量
- 开发时间: 2-3 天
- 测试时间: 0.5 天
- **总计**: 2.5-3.5 天

---

### Task Group 2: AI 服务扩展系统 🤖 **P1 - 高优先级**

#### 背景
前端已实现（Phase 9.2）：
- ServiceMarket.jsx - 服务市场
- ServiceCard.jsx - 服务卡片
- PPTGenerator.jsx - PPT 生成器
- ModelSwitcher.jsx - 模型切换器

#### 需要实现的后端接口

##### 2.1 ServiceController - 服务控制器
```java
@RestController
@RequestMapping("/api/services")
public class ServiceController {
    
    // GET /api/services - 获取服务列表
    @GetMapping
    public ResponseEntity<List<ServiceDTO>> getServices(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Boolean installed
    );
    
    // GET /api/services/{id} - 获取服务详情
    @GetMapping("/{id}")
    public ResponseEntity<ServiceDetailDTO> getServiceDetail(@PathVariable String id);
    
    // POST /api/services/{id}/install - 安装服务
    @PostMapping("/{id}/install")
    public ResponseEntity<InstallResult> installService(@PathVariable String id);
    
    // POST /api/services/{id}/uninstall - 卸载服务
    @PostMapping("/{id}/uninstall")
    public ResponseEntity<Void> uninstallService(@PathVariable String id);
    
    // PUT /api/services/{id}/config - 更新服务配置
    @PutMapping("/{id}/config")
    public ResponseEntity<Void> updateServiceConfig(
        @PathVariable String id,
        @RequestBody Map<String, Object> config
    );
    
    // POST /api/services/ppt/generate - 生成 PPT
    @PostMapping("/ppt/generate")
    public ResponseEntity<PPTGenerateResult> generatePPT(
        @RequestBody PPTGenerateRequest request
    );
    
    // POST /api/services/model/switch - 切换模型
    @PostMapping("/model/switch")
    public ResponseEntity<Void> switchModel(@RequestBody ModelSwitchRequest request);
}
```

##### 2.2 数据模型
```java
// 服务定义（可以是配置文件或数据库）
@Data
public class AIService {
    private String id;              // 服务ID
    private String name;            // 服务名称
    private String description;     // 描述
    private String category;        // 分类
    private String version;         // 版本
    private boolean installed;      // 是否已安装
    private Map<String, Object> config;  // 配置
    private String icon;            // 图标
    private List<String> features;  // 功能列表
}

// PPT生成请求
@Data
public class PPTGenerateRequest {
    private String topic;           // 主题
    private String content;         // 内容
    private int slides;             // 页数
    private String template;        // 模板
    private String style;           // 风格
}
```

##### 2.3 服务层
```java
@Service
public class AIServiceManager {
    // 获取可用服务列表
    List<ServiceDTO> getAvailableServices();
    
    // 安装服务（下载、配置）
    InstallResult installService(String serviceId);
    
    // 卸载服务
    void uninstallService(String serviceId);
    
    // 更新服务配置
    void updateServiceConfig(String serviceId, Map<String, Object> config);
    
    // 执行服务（通用接口）
    ServiceResult executeService(String serviceId, Map<String, Object> params);
}

@Service
public class PPTGeneratorService {
    // 生成 PPT（调用 LLM + PPT 库）
    PPTGenerateResult generatePPT(PPTGenerateRequest request);
}

@Service
public class ModelService {
    // 切换模型（本地/在线）
    void switchModel(String modelType);
    
    // 获取当前模型状态
    ModelStatus getCurrentModel();
}
```

##### 2.4 国际化
```yaml
# zh/zh-service.yml
service:
  install:
    success: "✅ 服务安装成功: {0}"
    failed: "❌ 服务安装失败: {0}"
  uninstall:
    success: "✅ 服务卸载成功: {0}"
  ppt:
    generating: "🎨 正在生成 PPT..."
    success: "✅ PPT 生成成功"
    failed: "❌ PPT 生成失败: {0}"
  model:
    switch_success: "✅ 模型切换成功: {0}"
    switch_failed: "❌ 模型切换失败: {0}"

# en/en-service.yml
service:
  install:
    success: "✅ Service installed successfully: {0}"
    failed: "❌ Failed to install service: {0}"
  uninstall:
    success: "✅ Service uninstalled successfully: {0}"
  ppt:
    generating: "🎨 Generating PPT..."
    success: "✅ PPT generated successfully"
    failed: "❌ Failed to generate PPT: {0}"
  model:
    switch_success: "✅ Model switched successfully: {0}"
    switch_failed: "❌ Failed to switch model: {0}"
```

##### 2.5 预计工作量
- 开发时间: 3-4 天（PPT 生成较复杂）
- 测试时间: 1 天
- **总计**: 4-5 天

---

### Task Group 3: 个人中心系统 👤 **P1 - 中优先级**

#### 背景
前端已实现（Phase 9.3）：
- UserProfile.jsx - 个人信息
- UsageStatistics.jsx - 使用统计
- AchievementPanel.jsx - 成就面板
- UserSettings.jsx - 用户设置

#### 需要实现的后端接口

##### 3.1 ProfileController - 个人中心控制器
```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    // GET /api/profile/info - 获取用户信息
    @GetMapping("/info")
    public ResponseEntity<UserProfileDTO> getUserInfo();
    
    // PUT /api/profile/info - 更新用户信息
    @PutMapping("/info")
    public ResponseEntity<Void> updateUserInfo(@RequestBody UserProfileRequest request);
    
    // GET /api/profile/{userId}/statistics - 获取使用统计
    @GetMapping("/{userId}/statistics")
    public ResponseEntity<UsageStatisticsDTO> getUsageStatistics(@PathVariable Long userId);
    
    // GET /api/profile/{userId}/contributions - 获取贡献统计
    @GetMapping("/{userId}/contributions")
    public ResponseEntity<ContributionDTO> getContributions(@PathVariable Long userId);
    
    // GET /api/profile/{userId}/achievements - 获取成就列表
    @GetMapping("/{userId}/achievements")
    public ResponseEntity<List<AchievementDTO>> getAchievements(@PathVariable Long userId);
    
    // PUT /api/profile/settings - 更新用户设置
    @PutMapping("/settings")
    public ResponseEntity<Void> updateSettings(@RequestBody UserSettingsRequest request);
}
```

##### 3.2 数据模型
```java
@Entity
@Table(name = "user_profiles")
@Data
public class UserProfile {
    @Id
    private Long userId;
    
    private String nickname;
    private String avatar;
    private String bio;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}

@Data
public class UsageStatisticsDTO {
    private Long totalQuestions;      // 总提问数
    private Long totalDocuments;      // 总文档数
    private Long totalAnswers;        // 总回答数
    private Long totalFeedbacks;      // 总反馈数
    private Map<String, Long> questionsByRole;  // 各角色提问数
    private List<DateCount> dailyQuestions;     // 每日提问趋势
}

@Entity
@Table(name = "achievements")
@Data
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String code;          // 成就代码
    private String name;          // 成就名称
    private String description;   // 描述
    private String icon;          // 图标
    private Integer points;       // 积分
}

@Entity
@Table(name = "user_achievements")
@Data
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long achievementId;
    private LocalDateTime unlockedAt;
}
```

##### 3.3 服务层
```java
@Service
public class ProfileService {
    // 获取用户信息
    UserProfileDTO getUserProfile(Long userId);
    
    // 更新用户信息
    void updateUserProfile(Long userId, UserProfileRequest request);
    
    // 获取使用统计（聚合多个数据源）
    UsageStatisticsDTO getUsageStatistics(Long userId);
    
    // 获取贡献统计
    ContributionDTO getContributions(Long userId);
    
    // 获取成就列表
    List<AchievementDTO> getAchievements(Long userId);
    
    // 更新用户设置
    void updateSettings(Long userId, UserSettingsRequest request);
}

@Service
public class AchievementService {
    // 检查并解锁成就
    void checkAndUnlockAchievements(Long userId);
    
    // 成就规则引擎
    List<Achievement> evaluateAchievements(Long userId);
}
```

##### 3.4 预计工作量
- 开发时间: 2-3 天
- 测试时间: 0.5 天
- **总计**: 2.5-3.5 天

---

### Task Group 4: 协作网络系统 🌐 **P1 - 中优先级**

#### 背景
前端已实现（Phase 8.5）：
- CollaborationPanel.jsx - 协作面板
- PeerList.jsx - 伙伴列表
- ConnectionCodeGenerator.jsx - 连接码生成器
- NetworkGraph.jsx - 网络拓扑图

#### 需要实现的后端接口

##### 4.1 CollaborationController - 协作控制器
```java
@RestController
@RequestMapping("/api/collaboration")
public class CollaborationController {
    
    // GET /collaboration/peers - 获取协作伙伴列表
    @GetMapping("/peers")
    public ResponseEntity<List<PeerDTO>> getPeers();
    
    // POST /collaboration/generate-code - 生成连接码
    @PostMapping("/generate-code")
    public ResponseEntity<ConnectionCodeDTO> generateCode();
    
    // POST /collaboration/connect - 使用连接码连接
    @PostMapping("/connect")
    public ResponseEntity<PeerDTO> connect(@RequestBody ConnectRequest request);
    
    // DELETE /collaboration/peers/{id} - 断开连接
    @DeleteMapping("/peers/{id}")
    public ResponseEntity<Void> disconnect(@PathVariable String id);
    
    // POST /collaboration/exchange - 知识交换
    @PostMapping("/exchange")
    public ResponseEntity<ExchangeResult> exchange(@RequestBody ExchangeRequest request);
    
    // GET /collaboration/contribution - 获取贡献统计
    @GetMapping("/contribution")
    public ResponseEntity<ContributionStatsDTO> getContribution();
    
    // GET /collaboration/network-graph - 获取网络拓扑
    @GetMapping("/network-graph")
    public ResponseEntity<NetworkGraphDTO> getNetworkGraph();
    
    // POST /collaboration/peers/{id}/sync - 同步数据
    @PostMapping("/peers/{id}/sync")
    public ResponseEntity<SyncResult> syncWith(@PathVariable String id);
    
    // GET /collaboration/exchange-history - 获取交换历史
    @GetMapping("/exchange-history")
    public ResponseEntity<List<ExchangeHistoryDTO>> getExchangeHistory();
    
    // GET /collaboration/topology - 获取拓扑
    @GetMapping("/topology")
    public ResponseEntity<TopologyDTO> getTopology();
    
    // GET /collaboration/sync-status - 获取同步状态
    @GetMapping("/sync-status")
    public ResponseEntity<SyncStatusDTO> getSyncStatus();
}
```

##### 4.2 数据模型
```java
@Entity
@Table(name = "collaboration_peers")
@Data
public class CollaborationPeer {
    @Id
    private String peerId;          // 伙伴ID
    
    private String peerName;        // 伙伴名称
    private String peerAddress;     // 地址
    private String connectionCode;  // 连接码
    private String status;          // 状态 (active/inactive)
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncAt;
}

@Entity
@Table(name = "knowledge_exchanges")
@Data
public class KnowledgeExchange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String peerId;
    private String exchangeType;    // send/receive
    private Integer documentCount;
    private Long dataSize;
    private String status;          // pending/approved/rejected
    private LocalDateTime createdAt;
}
```

##### 4.3 服务层
```java
@Service
public class CollaborationService {
    // P2P 连接管理
    ConnectionCodeDTO generateConnectionCode();
    PeerDTO connectWithCode(String code);
    void disconnect(String peerId);
    
    // 知识交换
    ExchangeResult exchangeKnowledge(ExchangeRequest request);
    
    // 数据同步
    SyncResult syncWithPeer(String peerId);
    
    // 网络拓扑
    NetworkGraphDTO getNetworkGraph();
    TopologyDTO getTopology();
    
    // 统计
    ContributionStatsDTO getContributionStats();
}
```

##### 4.4 预计工作量
- 开发时间: 4-5 天（P2P 较复杂）
- 测试时间: 1 天
- **总计**: 5-6 天

---

### Task Group 5: 系统管理 API ⚙️ **P2 - 低优先级**

#### 背景
前端已实现（Phase 9.4）：
- AdminPanel.jsx - 管理面板
- LogViewer.jsx - 日志查看器
- MonitorDashboard.jsx - 监控面板
- HealthCheck.jsx - 健康检查

#### 需要实现的后端接口

##### 5.1 AdminController - 管理控制器
```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    // PUT /api/admin/system-config - 更新系统配置
    @PutMapping("/system-config")
    public ResponseEntity<Void> updateSystemConfig(@RequestBody SystemConfigRequest request);
    
    // PUT /api/admin/model-config - 更新模型配置
    @PutMapping("/model-config")
    public ResponseEntity<Void> updateModelConfig(@RequestBody ModelConfigRequest request);
    
    // GET /api/admin/logs - 获取日志
    @GetMapping("/logs")
    public ResponseEntity<PageResult<LogEntryDTO>> getLogs(
        @RequestParam(required = false) String level,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size
    );
    
    // GET /api/admin/metrics - 获取监控指标
    @GetMapping("/metrics")
    public ResponseEntity<SystemMetricsDTO> getMetrics();
    
    // GET /api/admin/health - 健康检查
    @GetMapping("/health")
    public ResponseEntity<HealthCheckDTO> healthCheck();
}
```

##### 5.2 预计工作量
- 开发时间: 2-3 天
- 测试时间: 0.5 天
- **总计**: 2.5-3.5 天

---

## 📅 开发时间表

### 建议的开发顺序（按优先级）

#### Week 1: 愿望单系统（P1）
- **Day 1-2**: 数据模型 + Controller + Service
- **Day 2-3**: 投票逻辑 + 评论系统
- **Day 3**: 测试 + 联调

#### Week 2: AI 服务扩展（P1）
- **Day 1-2**: 服务框架 + Controller
- **Day 3-4**: PPT 生成器实现
- **Day 4-5**: 模型切换 + 测试

#### Week 3: 个人中心 + 协作网络（P1）
- **Day 1-3**: 个人中心（统计 + 成就系统）
- **Day 3-5**: 协作网络（P2P + 数据同步）

#### Week 4: 系统管理 + 测试（P2）
- **Day 1-2**: 系统管理 API
- **Day 3-5**: 全面测试 + Bug 修复

---

## 📊 总体工作量估算

```yaml
任务组统计:
  Task Group 1 (愿望单):     2.5-3.5 天
  Task Group 2 (AI服务):     4-5 天
  Task Group 3 (个人中心):   2.5-3.5 天
  Task Group 4 (协作网络):   5-6 天
  Task Group 5 (系统管理):   2.5-3.5 天
  
总计: 17-21.5 天 (约 3-4 周)
```

---

## ✅ 验收标准

### 功能验收
- [ ] 所有 API 接口可正常调用
- [ ] 前后端联调通过
- [ ] 核心功能流程可用
- [ ] 数据正确保存和查询

### 代码质量验收
- [ ] 遵守 `20251209-23-00-00-CODE_STANDARDS.md` 规范
- [ ] 使用 Lombok @Data 注解
- [ ] 注释格式: 中文(英文)
- [ ] 日志国际化: I18n.get()
- [ ] 字符串提取到 YAML

### 性能验收
- [ ] API 响应时间 < 300ms (P95)
- [ ] 数据库查询优化
- [ ] 无明显性能瓶颈

### 测试验收
- [ ] 单元测试覆盖率 > 60%
- [ ] 关键功能集成测试通过
- [ ] 前后端联调测试通过

---

## 🎯 下一步行动

### 立即开始
```
开始实施 Task Group 1: 愿望单系统
```

**告诉我**：
```
"开始实现 Task Group 1.1: WishController"
```

或者：
```
"先从数据模型开始: Wish、WishVote、WishComment"
```

---

## 📝 备注

### 技术选型建议
- **数据库**: H2（开发）/ PostgreSQL（生产）
- **缓存**: Spring Cache + Caffeine
- **事务**: Spring @Transactional
- **验证**: Spring Validation
- **文档**: SpringDoc (OpenAPI)

### 开发建议
1. **先数据模型，后接口**: 先设计好实体类和 DTO，再实现 Controller
2. **服务层先行**: 业务逻辑在 Service 层实现，Controller 只做转发
3. **国际化同步**: 开发时同步创建 YAML 国际化文件
4. **测试驱动**: 关键功能先写测试用例

---

**文档版本**: v1.0  
**创建日期**: 2025-12-12  
**作者**: AI Reviewer Team  
**状态**: 📋 等待执行

---

**准备开始了吗？让我们从 Task Group 1 开始！** 🚀

