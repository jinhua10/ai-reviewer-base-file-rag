package top.yumbo.ai.rag.service.wish;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.wish.Wish;
import top.yumbo.ai.rag.model.wish.WishComment;
import top.yumbo.ai.rag.model.wish.dto.*;
import top.yumbo.ai.rag.model.wish.request.CommentRequest;
import top.yumbo.ai.rag.model.wish.request.VoteRequest;
import top.yumbo.ai.rag.model.wish.request.WishSubmitRequest;
import top.yumbo.ai.rag.spring.boot.autoconfigure.SimpleRAGService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 愿望单服务 (Wish Service)
 *
 * 基于文档管理系统的愿望单服务
 * (Wish service based on document management system)
 *
 * 所有愿望数据存储为 JSON 文档，利用现有的文档管理和 RAG 检索功能
 * (All wish data stored as JSON documents, utilizing existing document management and RAG retrieval)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@Service
public class WishService {

    private final SimpleRAGService ragService;
    private final ObjectMapper objectMapper;

    // 文档类型标识 (Document type identifier)
    private static final String DOC_TYPE_WISH = "wish";

    public WishService(SimpleRAGService ragService) {
        this.ragService = ragService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 提交新愿望 (Submit new wish)
     *
     * @param request 愿望提交请求 (Wish submit request)
     * @return 愿望 DTO (Wish DTO)
     */
    public WishDTO submitWish(WishSubmitRequest request) {
        log.info(I18N.get("wish.submit.start"), request.getTitle());

        try {
            // 创建愿望对象 (Create wish object)
            Wish wish = new Wish();
            wish.setId(UUID.randomUUID().toString());
            wish.setTitle(request.getTitle());
            wish.setDescription(request.getDescription());
            wish.setCategory(request.getCategory());
            wish.setStatus("pending");
            wish.setSubmitUserId(request.getSubmitUserId() != null ? request.getSubmitUserId().toString() : "anonymous");
            wish.setSubmitUsername(request.getSubmitUsername() != null ? request.getSubmitUsername() : "匿名用户");
            wish.setCreatedAt(LocalDateTime.now());

            // 转换为 JSON 文档存储 (Convert to JSON document for storage)
            String wishJson = objectMapper.writeValueAsString(wish);

            // 创建文档对象 (Create document object)
            Document doc = new Document();
            doc.setId(wish.getId());
            doc.setTitle(wish.getTitle());
            doc.setContent(wishJson); // 完整的 JSON 作为内容

            // 设置元数据 (Set metadata)
            Map<String, Object> metadata = wish.toMetadata();
            metadata.put("searchableContent", wish.getTitle() + " " + wish.getDescription());
            doc.setMetadata(metadata);

            // 索引到文档管理系统 (Index to document management system)
            ragService.getRag().index(doc);

            log.info(I18N.get("wish.submit.success"), wish.getId());
            return toDTO(wish);

        } catch (Exception e) {
            log.error(I18N.get("wish.submit.failed", e.getMessage()), e);
            throw new RuntimeException(I18N.get("wish.submit.failed", e.getMessage()), e);
        }
    }

    /**
     * 获取愿望列表 (Get wish list)
     *
     * @param status 状态筛选 (Status filter)
     * @param category 分类筛选 (Category filter)
     * @param sortBy 排序方式 (Sort by)
     * @param keyword 搜索关键词 (Search keyword)
     * @param page 页码 (Page number)
     * @param size 每页大小 (Page size)
     * @return 分页结果 (Page result)
     */
    public Map<String, Object> getWishes(String status, String category, String sortBy,
                                         String keyword, int page, int size) {
        log.info(I18N.get("wish.list.loading"), status, category);

        try {
            // 构建查询条件 (Build query condition)
            String query = buildQuery(status, category, keyword);

            // 使用 RAG 检索 (Use RAG retrieval)
            List<Document> documents = ragService.search(query, 1000); // 先获取较多结果

            // 过滤并转换为 Wish 对象 (Filter and convert to Wish objects)
            List<Wish> wishes = documents.stream()
                .filter(doc -> {
                    Object type = doc.getMetadata().get("type");
                    return DOC_TYPE_WISH.equals(type);
                })
                .map(this::documentToWish)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 额外过滤 (Additional filtering)
            wishes = filterWishes(wishes, status, category);

            // 排序 (Sort)
            sortWishes(wishes, sortBy);

            // 分页 (Pagination)
            int total = wishes.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<Wish> pageWishes = wishes.subList(Math.min(start, total), end);

            // 转换为 DTO (Convert to DTO)
            List<WishDTO> wishDTOs = pageWishes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            // 构建分页结果 (Build page result)
            Map<String, Object> result = new HashMap<>();
            result.put("content", wishDTOs);
            result.put("totalElements", total);
            result.put("totalPages", (total + size - 1) / size);
            result.put("currentPage", page);
            result.put("pageSize", size);

            log.info(I18N.get("wish.list.success"), total);
            return result;

        } catch (Exception e) {
            log.error(I18N.get("wish.list.failed", e.getMessage()), e);
            throw new RuntimeException(I18N.get("wish.list.failed", e.getMessage()), e);
        }
    }

    /**
     * 获取愿望详情 (Get wish detail)
     *
     * @param id 愿望 ID (Wish ID)
     * @param userId 当前用户 ID (Current user ID)
     * @return 愿望详情 DTO (Wish detail DTO)
     */
    public WishDetailDTO getWishDetail(String id, String userId) {
        log.info(I18N.get("wish.detail.loading"), id);

        try {
            // 从文档管理系统获取 (Get from document management system)
            Document doc = ragService.getDocument(id);
            if (doc == null) {
                throw new RuntimeException(I18N.get("wish.not_found", id));
            }

            // 转换为 Wish 对象 (Convert to Wish object)
            Wish wish = documentToWish(doc);
            if (wish == null) {
                throw new RuntimeException(I18N.get("wish.not_found", id));
            }

            // 转换为详情 DTO (Convert to detail DTO)
            WishDetailDTO detailDTO = toDetailDTO(wish);

            // 设置当前用户的投票状态 (Set current user's vote status)
            if (userId != null && wish.getVotes() != null) {
                detailDTO.setCurrentUserVote(wish.getVotes().get(userId));
            }

            log.info(I18N.get("wish.detail.success"), id);
            return detailDTO;

        } catch (Exception e) {
            log.error(I18N.get("wish.detail.failed", id, e.getMessage()), e);
            throw new RuntimeException(I18N.get("wish.detail.failed", id, e.getMessage()), e);
        }
    }

    /**
     * 投票 (Vote)
     *
     * @param wishId 愿望 ID (Wish ID)
     * @param request 投票请求 (Vote request)
     * @return 投票结果 (Vote result)
     */
    public VoteResultDTO vote(String wishId, VoteRequest request) {
        log.info(I18N.get("wish.vote.start"), wishId, request.getVoteType());

        try {
            // 获取愿望文档 (Get wish document)
            Document doc = ragService.getDocument(wishId);
            if (doc == null) {
                throw new RuntimeException(I18N.get("wish.not_found", wishId));
            }

            Wish wish = documentToWish(doc);
            if (wish == null) {
                throw new RuntimeException(I18N.get("wish.not_found", wishId));
            }
            String userId = request.getUserId() != null ? request.getUserId().toString() : "anonymous";

            // 检查是否已投票 (Check if already voted)
            Map<String, String> votes = wish.getVotes();
            if (votes == null) {
                votes = new HashMap<>();
                wish.setVotes(votes);
            }
            String existingVote = votes.get(userId);

            VoteResultDTO result = new VoteResultDTO();

            if (request.getVoteType().equals(existingVote)) {
                // 取消投票 (Cancel vote)
                votes.remove(userId);
                if ("up".equals(existingVote)) {
                    wish.setUpVotes(wish.getUpVotes() - 1);
                } else {
                    wish.setDownVotes(wish.getDownVotes() - 1);
                }
                result.setMessage(I18N.get("wish.vote.cancelled"));
                result.setCurrentVoteType(null);
            } else {
                // 修改或新增投票 (Modify or add vote)
                if (existingVote != null) {
                    // 修改投票 (Modify vote)
                    if ("up".equals(existingVote)) {
                        wish.setUpVotes(wish.getUpVotes() - 1);
                    } else {
                        wish.setDownVotes(wish.getDownVotes() - 1);
                    }
                }

                votes.put(userId, request.getVoteType());
                if ("up".equals(request.getVoteType())) {
                    wish.setUpVotes(wish.getUpVotes() + 1);
                } else {
                    wish.setDownVotes(wish.getDownVotes() + 1);
                }
                result.setMessage(I18N.get("wish.vote.success"));
                result.setCurrentVoteType(request.getVoteType());
            }

            // 更新投票数 (Update vote count)
            wish.setVoteCount(wish.getUpVotes() - wish.getDownVotes());
            wish.setUpdatedAt(LocalDateTime.now());

            // 更新文档 (Update document)
            updateWishDocument(wish);

            result.setSuccess(true);
            result.setVoteCount(wish.getVoteCount());
            result.setUpVotes(wish.getUpVotes());
            result.setDownVotes(wish.getDownVotes());

            log.info(I18N.get("wish.vote.success"), wishId);
            return result;

        } catch (Exception e) {
            log.error(I18N.get("wish.vote.failed", wishId, e.getMessage()), e);
            VoteResultDTO result = new VoteResultDTO();
            result.setSuccess(false);
            result.setMessage(I18N.get("wish.vote.failed", wishId, e.getMessage()));
            return result;
        }
    }

    /**
     * 添加评论 (Add comment)
     *
     * @param wishId 愿望 ID (Wish ID)
     * @param request 评论请求 (Comment request)
     * @return 评论 DTO (Comment DTO)
     */
    public CommentDTO addComment(String wishId, CommentRequest request) {
        log.info(I18N.get("wish.comment.add_start"), wishId);

        try {
            // 获取愿望文档 (Get wish document)
            Document doc = ragService.getDocument(wishId);
            if (doc == null) {
                throw new RuntimeException(I18N.get("wish.not_found", wishId));
            }

            Wish wish = documentToWish(doc);
            if (wish == null) {
                throw new RuntimeException(I18N.get("wish.not_found", wishId));
            }

            // 创建评论 (Create comment)
            WishComment comment = new WishComment();
            comment.setId(UUID.randomUUID().toString());
            comment.setUserId(request.getUserId() != null ? request.getUserId().toString() : "anonymous");
            comment.setUsername(request.getUsername() != null ? request.getUsername() : "匿名用户");
            comment.setParentId(request.getParentId() != null ? request.getParentId().toString() : null);
            comment.setContent(request.getContent());
            comment.setCreatedAt(LocalDateTime.now());

            // 评论存储在 wish 的 metadata 中 (Store comments in wish metadata)
            Map<String, Object> metadata = doc.getMetadata();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> comments = (List<Map<String, Object>>) metadata.get("comments");
            if (comments == null) {
                comments = new ArrayList<>();
                metadata.put("comments", comments);
            }
            comments.add(commentToMap(comment));

            // 更新评论数 (Update comment count)
            wish.setCommentCount(wish.getCommentCount() + 1);
            wish.setUpdatedAt(LocalDateTime.now());

            // 更新文档 (Update document)
            updateWishDocument(wish);

            log.info(I18N.get("wish.comment.add_success"), wishId);
            return toCommentDTO(comment);

        } catch (Exception e) {
            log.error(I18N.get("wish.comment.add_failed", wishId, e.getMessage()), e);
            throw new RuntimeException(I18N.get("wish.comment.add_failed", wishId, e.getMessage()), e);
        }
    }

    /**
     * 获取评论列表 (Get comments)
     *
     * @param wishId 愿望 ID (Wish ID)
     * @return 评论列表 (Comment list)
     */
    public List<CommentDTO> getComments(String wishId) {
        log.info(I18N.get("wish.comment.get_start"), wishId);

        try {
            // 获取愿望文档 (Get wish document)
            Document doc = ragService.getDocument(wishId);
            if (doc == null) {
                throw new RuntimeException(I18N.get("wish.not_found", wishId));
            }

            // 从 metadata 读取评论 (Read comments from metadata)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commentsData =
                (List<Map<String, Object>>) doc.getMetadata().get("comments");

            if (commentsData == null || commentsData.isEmpty()) {
                log.info(I18N.get("wish.comment.empty"), wishId);
                return new ArrayList<>();
            }

            // 转换为 WishComment 对象列表 (Convert to WishComment objects)
            List<WishComment> comments = commentsData.stream()
                .map(this::mapToComment)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 构建评论树 (Build comment tree)
            List<CommentDTO> commentTree = buildCommentTree(comments);

            log.info(I18N.get("wish.comment.get_success"), wishId, commentTree.size());
            return commentTree;

        } catch (Exception e) {
            log.error(I18N.get("wish.comment.get_failed", wishId, e.getMessage()), e);
            throw new RuntimeException(I18N.get("wish.comment.get_failed", wishId, e.getMessage()), e);
        }
    }

    /**
     * 点赞评论 (Like comment)
     *
     * @param commentId 评论 ID (Comment ID)
     * @param userId 用户 ID (User ID)
     * @return 点赞结果 (Like result)
     */
    public Map<String, Object> likeComment(String commentId, String userId) {
        log.info(I18N.get("wish.comment.like_start"), commentId, userId);

        try {
            // 查找包含该评论的所有愿望 (Find all wishes containing the comment)
            List<Document> allWishes = ragService.search("type:wish", 1000);

            Document targetWishDoc = null;
            WishComment targetComment = null;

            // 遍历所有愿望，查找评论 (Iterate all wishes to find the comment)
            for (Document doc : allWishes) {
                if (!DOC_TYPE_WISH.equals(doc.getMetadata().get("type"))) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> commentsData =
                    (List<Map<String, Object>>) doc.getMetadata().get("comments");

                if (commentsData != null) {
                    for (Map<String, Object> commentData : commentsData) {
                        if (commentId.equals(commentData.get("id"))) {
                            targetWishDoc = doc;
                            targetComment = mapToComment(commentData);
                            break;
                        }
                    }
                }

                if (targetComment != null) {
                    break;
                }
            }

            Map<String, Object> result = new HashMap<>();

            if (targetComment == null || targetWishDoc == null) {
                result.put("success", false);
                result.put("message", I18N.get("wish.comment.not_found", commentId));
                return result;
            }

            // 检查是否已点赞 (Check if already liked)
            boolean alreadyLiked = targetComment.getLikedBy().contains(userId);

            if (alreadyLiked) {
                // 取消点赞 (Unlike)
                targetComment.getLikedBy().remove(userId);
                targetComment.setLikeCount(targetComment.getLikeCount() - 1);
                result.put("action", "unliked");
                result.put("message", I18N.get("wish.comment.unliked"));
            } else {
                // 点赞 (Like)
                targetComment.getLikedBy().add(userId);
                targetComment.setLikeCount(targetComment.getLikeCount() + 1);
                result.put("action", "liked");
                result.put("message", I18N.get("wish.comment.liked"));
            }

            // 更新文档 (Update document)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commentsData =
                (List<Map<String, Object>>) targetWishDoc.getMetadata().get("comments");

            // 找到并更新评论 (Find and update the comment)
            for (int i = 0; i < commentsData.size(); i++) {
                if (commentId.equals(commentsData.get(i).get("id"))) {
                    commentsData.set(i, commentToMap(targetComment));
                    break;
                }
            }

            // 重新索引文档 (Re-index document)
            Wish wish = documentToWish(targetWishDoc);
            if (wish != null) {
                wish.setUpdatedAt(LocalDateTime.now());
                updateWishDocument(wish);
            }

            result.put("success", true);
            result.put("likeCount", targetComment.getLikeCount());
            result.put("commentId", commentId);

            log.info(I18N.get("wish.comment.like_success_detail"), commentId, result.get("action"));
            return result;

        } catch (Exception e) {
            log.error(I18N.get("wish.comment.like_failed", commentId, e.getMessage()), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", I18N.get("wish.comment.like_failed", commentId, e.getMessage()));
            return result;
        }
    }

    /**
     * 获取排行榜 (Get ranking)
     *
     * @param limit 数量限制 (Limit)
     * @return 愿望列表 (Wish list)
     */
    public List<WishDTO> getRanking(int limit) {
        log.info(I18N.get("wish.ranking.loading"), limit);

        try {
            // 获取所有愿望 (Get all wishes)
            List<Document> documents = ragService.search("type:wish", 1000);

            // 转换并排序 (Convert and sort)
            List<Wish> wishes = documents.stream()
                .filter(doc -> DOC_TYPE_WISH.equals(doc.getMetadata().get("type")))
                .map(this::documentToWish)
                .filter(Objects::nonNull)
                .sorted((w1, w2) -> w2.getVoteCount().compareTo(w1.getVoteCount()))
                .limit(limit)
                .collect(Collectors.toList());

            List<WishDTO> result = wishes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            log.info(I18N.get("wish.ranking.success"), result.size());
            return result;

        } catch (Exception e) {
            log.error(I18N.get("wish.ranking.failed", e.getMessage()), e);
            throw new RuntimeException(I18N.get("wish.ranking.failed", e.getMessage()), e);
        }
    }

    // ==================== 私有辅助方法 (Private helper methods) ====================

    /**
     * 构建查询语句 (Build query)
     */
    private String buildQuery(String status, String category, String keyword) {
        StringBuilder query = new StringBuilder("type:wish");

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.append(" ").append(keyword);
        }

        return query.toString();
    }

    /**
     * 过滤愿望 (Filter wishes)
     */
    private List<Wish> filterWishes(List<Wish> wishes, String status, String category) {
        return wishes.stream()
            .filter(wish -> status == null || status.equals(wish.getStatus()))
            .filter(wish -> category == null || category.equals(wish.getCategory()))
            .collect(Collectors.toList());
    }

    /**
     * 排序愿望 (Sort wishes)
     */
    private void sortWishes(List<Wish> wishes, String sortBy) {
        if ("voteCount".equals(sortBy)) {
            wishes.sort((w1, w2) -> w2.getVoteCount().compareTo(w1.getVoteCount()));
        } else if ("createdAt".equals(sortBy)) {
            wishes.sort((w1, w2) -> w2.getCreatedAt().compareTo(w1.getCreatedAt()));
        } else {
            // 默认按创建时间倒序 (Default: sort by created time descending)
            wishes.sort((w1, w2) -> w2.getCreatedAt().compareTo(w1.getCreatedAt()));
        }
    }

    /**
     * 文档转 Wish 对象 (Document to Wish)
     */
    private Wish documentToWish(Document doc) {
        try {
            return objectMapper.readValue(doc.getContent(), Wish.class);
        } catch (Exception e) {
            log.error("Failed to parse wish document: {}", doc.getId(), e);
            return null;
        }
    }

    /**
     * 更新愿望文档 (Update wish document)
     */
    private void updateWishDocument(Wish wish) throws Exception {
        String wishJson = objectMapper.writeValueAsString(wish);

        Document doc = new Document();
        doc.setId(wish.getId());
        doc.setTitle(wish.getTitle());
        doc.setContent(wishJson);
        doc.setMetadata(wish.toMetadata());

        ragService.getRag().index(doc); // 重新索引 (Re-index)
    }

    /**
     * Wish 转 DTO
     */
    private WishDTO toDTO(Wish wish) {
        WishDTO dto = new WishDTO();
        dto.setId(Long.parseLong(wish.getId().hashCode() + ""));
        dto.setTitle(wish.getTitle());
        dto.setDescription(wish.getDescription().length() > 200 ?
            wish.getDescription().substring(0, 200) + "..." : wish.getDescription());
        dto.setCategory(wish.getCategory());
        dto.setStatus(wish.getStatus());
        dto.setSubmitUsername(wish.getSubmitUsername());
        dto.setVoteCount(wish.getVoteCount());
        dto.setUpVotes(wish.getUpVotes());
        dto.setDownVotes(wish.getDownVotes());
        dto.setCommentCount(wish.getCommentCount());
        dto.setCreatedAt(wish.getCreatedAt());
        dto.setUpdatedAt(wish.getUpdatedAt());
        return dto;
    }

    /**
     * Wish 转详情 DTO
     */
    private WishDetailDTO toDetailDTO(Wish wish) {
        WishDetailDTO dto = new WishDetailDTO();
        dto.setId(Long.parseLong(wish.getId().hashCode() + ""));
        dto.setTitle(wish.getTitle());
        dto.setDescription(wish.getDescription());
        dto.setCategory(wish.getCategory());
        dto.setStatus(wish.getStatus());
        dto.setSubmitUserId(wish.getSubmitUserId() != null ? Long.parseLong(wish.getSubmitUserId()) : null);
        dto.setSubmitUsername(wish.getSubmitUsername());
        dto.setVoteCount(wish.getVoteCount());
        dto.setUpVotes(wish.getUpVotes());
        dto.setDownVotes(wish.getDownVotes());
        dto.setCommentCount(wish.getCommentCount());
        dto.setCreatedAt(wish.getCreatedAt());
        dto.setUpdatedAt(wish.getUpdatedAt());
        return dto;
    }

    /**
     * Comment 转 Map
     */
    private Map<String, Object> commentToMap(WishComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("userId", comment.getUserId());
        map.put("username", comment.getUsername());
        map.put("parentId", comment.getParentId());
        map.put("content", comment.getContent());
        map.put("likeCount", comment.getLikeCount());
        map.put("likedBy", new ArrayList<>(comment.getLikedBy()));
        map.put("createdAt", comment.getCreatedAt().toString());
        return map;
    }

    /**
     * Map 转 Comment (Map to Comment)
     */
    private WishComment mapToComment(Map<String, Object> map) {
        try {
            WishComment comment = new WishComment();
            comment.setId((String) map.get("id"));
            comment.setUserId((String) map.get("userId"));
            comment.setUsername((String) map.get("username"));
            comment.setParentId((String) map.get("parentId"));
            comment.setContent((String) map.get("content"));

            Object likeCount = map.get("likeCount");
            comment.setLikeCount(likeCount != null ? (Integer) likeCount : 0);

            @SuppressWarnings("unchecked")
            List<String> likedBy = (List<String>) map.get("likedBy");
            if (likedBy != null) {
                comment.setLikedBy(new HashSet<>(likedBy));
            }

            String createdAtStr = (String) map.get("createdAt");
            if (createdAtStr != null) {
                comment.setCreatedAt(LocalDateTime.parse(createdAtStr));
            }

            return comment;
        } catch (Exception e) {
            log.error("Failed to convert map to comment: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建评论树 (Build comment tree)
     *
     * 将扁平的评论列表转换为嵌套的树形结构
     * (Convert flat comment list to nested tree structure)
     */
    private List<CommentDTO> buildCommentTree(List<WishComment> comments) {
        // 分离顶级评论和回复 (Separate top-level comments and replies)
        Map<String, WishComment> commentMap = new HashMap<>();
        List<WishComment> topLevelComments = new ArrayList<>();

        for (WishComment comment : comments) {
            commentMap.put(comment.getId(), comment);
            if (comment.getParentId() == null || comment.getParentId().isEmpty()) {
                topLevelComments.add(comment);
            }
        }

        // 构建树形结构 (Build tree structure)
        for (WishComment comment : comments) {
            if (comment.getParentId() != null && !comment.getParentId().isEmpty()) {
                WishComment parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    parent.getReplies().add(comment);
                }
            }
        }

        // 转换为 DTO (Convert to DTO)
        return topLevelComments.stream()
            .map(this::commentToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Comment 转 DTO（递归转换子评论）
     * (Comment to DTO, recursively convert child comments)
     */
    private CommentDTO commentToDTO(WishComment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(Long.parseLong(String.valueOf(Math.abs(comment.getId().hashCode()))));
        dto.setUserId(comment.getUserId() != null ? Long.parseLong(String.valueOf(Math.abs(comment.getUserId().hashCode()))) : null);
        dto.setUsername(comment.getUsername());
        dto.setParentId(comment.getParentId() != null ? Long.parseLong(String.valueOf(Math.abs(comment.getParentId().hashCode()))) : null);
        dto.setContent(comment.getContent());
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreatedAt(comment.getCreatedAt());

        // 递归转换子评论 (Recursively convert child comments)
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDTO> replyDTOs = comment.getReplies().stream()
                .map(this::commentToDTO)
                .collect(Collectors.toList());
            dto.setReplies(replyDTOs);
        }

        return dto;
    }

    /**
     * Comment 转 DTO（旧版本，保留兼容）
     * (Comment to DTO, old version for compatibility)
     */
    private CommentDTO toCommentDTO(WishComment comment) {
        return commentToDTO(comment);
    }
}

