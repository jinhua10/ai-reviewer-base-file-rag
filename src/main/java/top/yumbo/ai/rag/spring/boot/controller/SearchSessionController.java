package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.service.SearchSessionService;

/**
 * 搜索会话管理控制器 - 支持分页引用文档 / Search Session Management Controller - Support paginated document referencing
 *
 * @author AI Reviewer Team
 * @since 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/api/search/session")
@CrossOrigin(origins = "*")
public class SearchSessionController {

    private final SearchSessionService sessionService;

    public SearchSessionController(SearchSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 获取当前批次文档 / Get current batch of documents
     */
    @GetMapping("/{sessionId}/current")
    public SearchSessionService.SessionDocuments getCurrentDocuments(
            @PathVariable String sessionId) {
        return sessionService.getCurrentDocuments(sessionId);
    }

    /**
     * 获取下一批文档 / Get next batch of documents
     */
    @PostMapping("/{sessionId}/next")
    public SearchSessionService.SessionDocuments getNextDocuments(
            @PathVariable String sessionId) {
        return sessionService.getNextDocuments(sessionId);
    }

    /**
     * 获取上一批文档 / Get previous batch of documents
     */
    @PostMapping("/{sessionId}/previous")
    public SearchSessionService.SessionDocuments getPreviousDocuments(
            @PathVariable String sessionId) {
        return sessionService.getPreviousDocuments(sessionId);
    }

    /**
     * 跳转到指定页 / Jump to specified page
     */
    @GetMapping("/{sessionId}/page/{page}")
    public SearchSessionService.SessionDocuments getPage(
            @PathVariable String sessionId,
            @PathVariable int page) {
        return sessionService.getDocumentsByPage(sessionId, page);
    }

    /**
     * 获取会话信息 / Get session information
     */
    @GetMapping("/{sessionId}/info")
    public SearchSessionService.SessionInfo getSessionInfo(
            @PathVariable String sessionId) {
        return sessionService.getSessionInfo(sessionId);
    }

    /**
     * 删除会话 / Delete session
     */
    @DeleteMapping("/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
    }
}

