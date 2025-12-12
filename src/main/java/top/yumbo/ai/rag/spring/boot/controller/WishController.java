package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.wish.dto.*;
import top.yumbo.ai.rag.model.wish.request.*;
import top.yumbo.ai.rag.service.wish.WishService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * GET /api/wishes?status=pending&category=feature&sortBy=voteCount&keyword=搜索关键词
     *
     * @param status 状态筛选 (Status filter): pending/accepted/rejected/completed
     * @param category 分类筛选 (Category filter): feature/bug/improvement/documentation
     * @param sortBy 排序方式 (Sort by): voteCount/createdAt
     * @param keyword 搜索关键词 (Search keyword)
     * @param page 页码 (Page number), 默认 0
     * @param size 每页大小 (Page size), 默认 20
     * @return 分页的愿望列表 (Paginated wish list)
     */
    @GetMapping
    public ResponseEntity<?> getWishes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info(I18N.get("wish.list.request"), status, category, sortBy);

        try {
            Map<String, Object> result = wishService.getWishes(status, category, sortBy, keyword, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("wish.list.error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取愿望详情 (Get wish detail)
     *
     * GET /api/wishes/{id}?userId=123
     *
     * @param id 愿望 ID (Wish ID)
     * @param userId 当前用户 ID，用于显示用户的投票状态 (Current user ID for showing vote status)
     * @return 愿望详情 (Wish detail)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWishDetail(
            @PathVariable String id,
            @RequestParam(required = false) String userId) {

        log.info(I18N.get("wish.detail.request"), id);

        try {
            WishDetailDTO detail = wishService.getWishDetail(id, userId);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error(I18N.get("wish.detail.error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 提交新愿望 (Submit new wish)
     *
     * POST /api/wishes
     * Body: {
     *   "title": "愿望标题",
     *   "description": "愿望描述",
     *   "category": "feature",
     *   "submitUserId": 123,
     *   "submitUsername": "用户名"
     * }
     *
     * @param request 愿望提交请求 (Wish submit request)
     * @return 创建的愿望 (Created wish)
     */
    @PostMapping
    public ResponseEntity<?> submitWish(@RequestBody WishSubmitRequest request) {

        log.info(I18N.get("wish.submit.request"), request.getTitle());

        try {
            WishDTO wish = wishService.submitWish(request);
            return ResponseEntity.ok(wish);
        } catch (Exception e) {
            log.error(I18N.get("wish.submit.error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 投票 (Vote)
     *
     * POST /api/wishes/{id}/vote
     * Body: {
     *   "voteType": "up",  // up 或 down
     *   "userId": 123
     * }
     *
     * @param id 愿望 ID (Wish ID)
     * @param request 投票请求 (Vote request)
     * @return 投票结果 (Vote result)
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<?> voteWish(
            @PathVariable String id,
            @RequestBody VoteRequest request) {

        log.info(I18N.get("wish.vote.request"), id, request.getVoteType());

        try {
            VoteResultDTO result = wishService.vote(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("wish.vote.error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取评论列表 (Get comments)
     *
     * GET /api/wishes/{id}/comments
     *
     * @param id 愿望 ID (Wish ID)
     * @return 评论列表 (Comment list)
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable String id) {

        log.info(I18N.get("wish.comment.get_request"), id);

        try {
            // TODO: 实现获取评论列表
            // 暂时返回空列表
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            log.error(I18N.get("wish.comment.get_error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 添加评论 (Add comment)
     *
     * POST /api/wishes/{id}/comments
     * Body: {
     *   "content": "评论内容",
     *   "parentId": 456,  // 可选，回复某条评论时提供
     *   "userId": 123,
     *   "username": "用户名"
     * }
     *
     * @param id 愿望 ID (Wish ID)
     * @param request 评论请求 (Comment request)
     * @return 创建的评论 (Created comment)
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String id,
            @RequestBody CommentRequest request) {

        log.info(I18N.get("wish.comment.add_request"), id);

        try {
            CommentDTO comment = wishService.addComment(id, request);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error(I18N.get("wish.comment.add_error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 点赞评论 (Like comment)
     *
     * POST /api/comments/{commentId}/like
     * Body: {
     *   "userId": 123
     * }
     *
     * @param commentId 评论 ID (Comment ID)
     * @return 操作结果 (Operation result)
     */
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(@PathVariable String commentId) {

        log.info(I18N.get("wish.comment.like_request"), commentId);

        try {
            // TODO: 实现点赞评论
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", I18N.get("wish.comment.like_success"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("wish.comment.like_error", commentId), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取愿望排行榜 (Get wish ranking)
     *
     * GET /api/wishes/ranking?limit=10
     *
     * @param limit 数量限制 (Limit), 默认 10
     * @return 排行榜愿望列表 (Ranking wish list)
     */
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking(@RequestParam(defaultValue = "10") int limit) {

        log.info(I18N.get("wish.ranking.request"), limit);

        try {
            List<WishDTO> ranking = wishService.getRanking(limit);
            return ResponseEntity.ok(ranking);
        } catch (Exception e) {
            log.error(I18N.get("wish.ranking.error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 创建错误响应 (Create error response)
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}

