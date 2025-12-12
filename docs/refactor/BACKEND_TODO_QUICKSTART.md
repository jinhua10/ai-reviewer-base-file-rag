# 后端 TODO 快速启动指南
# Backend TODO Quick Start Guide

> **创建日期**: 2025-12-12  
> **目标**: 快速开始后端 API 实现  
> **前置要求**: 已阅读 `20251209-23-00-00-CODE_STANDARDS.md`

---

## 🚀 快速开始

### 第一步：告诉 Copilot 遵守规范
```
"遵守 20251209-23-00-00-CODE_STANDARDS.md 中的所有代码规范"
```

### 第二步：选择任务
查看 `20251212-BACKEND_TODO_PLAN.md`，选择一个任务组开始实现。

**建议顺序**：
1. ✅ Task Group 1: 愿望单系统（P1，2.5-3.5天）← **推荐从这里开始**
2. Task Group 2: AI 服务扩展（P1，4-5天）
3. Task Group 3: 个人中心（P1，2.5-3.5天）
4. Task Group 4: 协作网络（P1，5-6天）
5. Task Group 5: 系统管理（P2，2.5-3.5天）

### 第三步：开始实现
```
"开始实现 Task Group 1: 愿望单系统"
```

---

## 📋 任务 1: 愿望单系统

### 实现顺序

#### 1.1 创建数据模型（30分钟）
```
"创建愿望单的实体类：Wish、WishVote、WishComment"
```

**位置**: `src/main/java/top/yumbo/ai/rag/model/wish/`

**文件**:
- `Wish.java` - 愿望实体
- `WishVote.java` - 投票记录
- `WishComment.java` - 评论记录

**规范要点**:
- ✅ 使用 `@Data` 注解
- ✅ 注释格式: `中文(英文)`
- ✅ 字段命名清晰
- ✅ 添加索引注解

#### 1.2 创建 DTO 类（20分钟）
```
"创建愿望单的 DTO 类：WishDTO、WishDetailDTO、CommentDTO"
```

**位置**: `src/main/java/top/yumbo/ai/rag/dto/wish/`

**文件**:
- `WishDTO.java` - 愿望列表 DTO
- `WishDetailDTO.java` - 愿望详情 DTO
- `CommentDTO.java` - 评论 DTO
- `VoteResult.java` - 投票结果

#### 1.3 创建 Repository（10分钟）
```
"创建愿望单的 Repository 接口"
```

**位置**: `src/main/java/top/yumbo/ai/rag/repository/wish/`

**文件**:
- `WishRepository.java`
- `WishVoteRepository.java`
- `WishCommentRepository.java`

#### 1.4 创建 Service 层（1小时）
```
"创建愿望单的 Service 类：WishService"
```

**位置**: `src/main/java/top/yumbo/ai/rag/service/wish/`

**文件**:
- `WishService.java` - 核心业务逻辑

**关键方法**:
- `getWishes()` - 获取列表（支持筛选排序）
- `submitWish()` - 提交愿望
- `voteWish()` - 投票（防重复）
- `addComment()` - 添加评论
- `getRanking()` - 排行榜

#### 1.5 创建 Controller（30分钟）
```
"创建愿望单的 Controller：WishController"
```

**位置**: `src/main/java/top/yumbo/ai/rag/spring/boot/controller/`

**文件**:
- `WishController.java`

**端点**:
- `GET /api/wishes` - 列表
- `GET /api/wishes/{id}` - 详情
- `POST /api/wishes` - 提交
- `POST /api/wishes/{id}/vote` - 投票
- `GET /api/wishes/{id}/comments` - 评论列表
- `POST /api/wishes/{id}/comments` - 添加评论
- `GET /api/wishes/ranking` - 排行榜

#### 1.6 创建国际化文件（15分钟）
```
"创建愿望单的国际化文件"
```

**位置**: 
- `src/main/resources/i18n/zh/zh-wish.yml`
- `src/main/resources/i18n/en/en-wish.yml`

**内容**: 参考 `20251212-BACKEND_TODO_PLAN.md` 中的示例

#### 1.7 测试（30分钟）
```
"创建愿望单的单元测试"
```

**位置**: `src/test/java/top/yumbo/ai/rag/service/wish/`

**测试用例**:
- 提交愿望测试
- 投票功能测试（包括防重复）
- 评论功能测试
- 排行榜测试

---

## 🔧 开发模板

### Controller 模板
```java
package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * 愿望单控制器 (Wish Controller)
 * 
 * 提供愿望单相关的 API 接口
 * (Provides wish-related API endpoints)
 * 
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@RestController
@RequestMapping("/api/wishes")
@CrossOrigin(origins = "*")
public class WishController {
    
    private final WishService wishService;
    
    public WishController(WishService wishService) {
        this.wishService = wishService;
    }
    
    /**
     * 获取愿望列表 (Get wish list)
     * 
     * @param status 状态筛选 (Status filter)
     * @param category 分类筛选 (Category filter)
     * @param sortBy 排序方式 (Sort by)
     * @param keyword 搜索关键词 (Search keyword)
     * @return 愿望列表 (Wish list)
     */
    @GetMapping
    public ResponseEntity<?> getWishes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info(I18N.get("wish.list.loading"), status, category);
        
        try {
            var result = wishService.getWishes(status, category, sortBy, keyword, page, size);
            log.info(I18N.get("wish.list.success"), result.getTotalElements());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("wish.list.failed", e.getMessage()), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", e.getMessage())
            );
        }
    }
    
    // 其他方法...
}
```

### Service 模板
```java
package top.yumbo.ai.rag.service.wish;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * 愿望单服务 (Wish Service)
 * 
 * 处理愿望单相关的业务逻辑
 * (Handles wish-related business logic)
 * 
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@Service
public class WishService {
    
    private final WishRepository wishRepository;
    private final WishVoteRepository voteRepository;
    private final WishCommentRepository commentRepository;
    
    public WishService(
            WishRepository wishRepository,
            WishVoteRepository voteRepository,
            WishCommentRepository commentRepository) {
        this.wishRepository = wishRepository;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
    }
    
    /**
     * 获取愿望列表 (Get wish list)
     */
    public Page<WishDTO> getWishes(
            String status, String category, String sortBy, 
            String keyword, int page, int size) {
        
        log.debug(I18N.get("wish.service.querying"), status, category);
        
        // 构建查询条件
        Pageable pageable = PageRequest.of(page, size, getSort(sortBy));
        
        // 执行查询
        Page<Wish> wishes = wishRepository.findAll(/* 条件 */, pageable);
        
        // 转换为 DTO
        return wishes.map(this::toDTO);
    }
    
    // 其他方法...
}
```

### Entity 模板
```java
package top.yumbo.ai.rag.model.wish;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 愿望实体 (Wish Entity)
 * 
 * 表示用户提交的功能愿望
 * (Represents user-submitted feature wishes)
 * 
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
@Entity
@Table(name = "wishes", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Wish {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 标题 (Title)
     */
    @Column(nullable = false, length = 200)
    private String title;
    
    /**
     * 描述 (Description)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    /**
     * 分类 (Category)
     */
    @Column(length = 50)
    private String category;
    
    /**
     * 状态 (Status): pending/accepted/rejected/completed
     */
    @Column(nullable = false, length = 20)
    private String status = "pending";
    
    /**
     * 提交用户 ID (Submit user ID)
     */
    @Column(name = "submit_user_id")
    private Long submitUserId;
    
    /**
     * 投票数 (Vote count)
     */
    @Column(name = "vote_count", nullable = false)
    private Integer voteCount = 0;
    
    /**
     * 赞成票 (Up votes)
     */
    @Column(name = "up_votes", nullable = false)
    private Integer upVotes = 0;
    
    /**
     * 反对票 (Down votes)
     */
    @Column(name = "down_votes", nullable = false)
    private Integer downVotes = 0;
    
    /**
     * 评论数 (Comment count)
     */
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;
    
    /**
     * 创建时间 (Created at)
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 更新时间 (Updated at)
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

## ✅ 检查清单

### 开发前
- [ ] 已阅读 `20251209-23-00-00-CODE_STANDARDS.md`
- [ ] 已阅读 `20251212-BACKEND_TODO_PLAN.md`
- [ ] 已选择要实现的任务组
- [ ] 已告诉 Copilot 遵守规范

### 开发中
- [ ] 使用 `@Data` 注解
- [ ] 注释格式: `中文(英文)`
- [ ] 日志使用 `I18N.get()`
- [ ] 字符串提取到 YAML
- [ ] Controller 添加 `@CrossOrigin`
- [ ] Service 添加事务注解（需要时）
- [ ] 添加适当的日志

### 开发后
- [ ] 编译通过（`mvn compile`）
- [ ] 单元测试通过
- [ ] API 可正常调用
- [ ] 前后端联调通过
- [ ] 国际化文件完整
- [ ] 代码格式化

---

## 📚 相关文档

- **代码规范**: `20251209-23-00-00-CODE_STANDARDS.md`
- **后端任务清单**: `20251212-BACKEND_TODO_PLAN.md`
- **前端计划**: `20251212-POLISH_AND_FRONTEND_PLAN.md`

---

## 🎯 开始实现

准备好了吗？选择一个任务开始：

```
"开始实现 Task Group 1: 愿望单系统"
```

或者从数据模型开始：

```
"创建愿望单的实体类：Wish、WishVote、WishComment"
```

---

**祝开发顺利！** 🚀

**文档版本**: v1.0  
**创建日期**: 2025-12-12  
**作者**: AI Reviewer Team

