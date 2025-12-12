package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.p2p.P2PCollaborationManager;
import top.yumbo.ai.rag.p2p.PeerConnection;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 协作 REST API 控制器 (Collaboration REST API Controller)
 *
 * 提供 P2P 协作功能的 HTTP 接口
 * (Provides HTTP interface for P2P collaboration features)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@RestController
@RequestMapping("/api/collaboration")
@CrossOrigin(origins = "*")
public class CollaborationController {

    // 暂时使用内存存储，实际应该注入 Service
    // (Temporarily use memory storage, should inject Service in production)
    private static final Map<String, P2PCollaborationManager> managers = new HashMap<>();

    private P2PCollaborationManager getOrCreateManager(String userId) {
        return managers.computeIfAbsent(userId, P2PCollaborationManager::new);
    }

    /**
     * 生成连接码 (Generate connection code)
     *
     * POST /api/collaboration/generate-code
     *
     * @return 连接码信息 (Connection code information)
     */
    @PostMapping("/generate-code")
    public ResponseEntity<GenerateCodeResponse> generateCode() {
        try {
            // 暂时使用固定用户ID，实际应该从认证信息获取
            // (Temporarily use fixed user ID, should get from authentication in production)
            String userId = "user-" + System.currentTimeMillis();

            P2PCollaborationManager manager = getOrCreateManager(userId);
            String code = manager.generateConnectionCode();

            log.info(I18N.get("collaboration.api.code_generated"), code);

            return ResponseEntity.ok(new GenerateCodeResponse(
                true,
                "连接码生成成功 (Connection code generated)",
                code,
                userId,
                LocalDateTime.now().plusMinutes(5) // 5分钟有效期
            ));

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.code_generate_failed"), e);
            return ResponseEntity.internalServerError()
                .body(new GenerateCodeResponse(false, e.getMessage(), null, null, null));
        }
    }

    /**
     * 使用连接码建立连接 (Connect using connection code)
     *
     * POST /api/collaboration/connect
     * Body: { "code": "xxx-xxx-xxx" }
     *
     * @param request 连接请求 (Connection request)
     * @return 连接结果 (Connection result)
     */
    @PostMapping("/connect")
    public ResponseEntity<ConnectResponse> connect(@RequestBody ConnectRequest request) {
        try {
            String userId = "user-current"; // 实际应从认证获取
            P2PCollaborationManager manager = getOrCreateManager(userId);

            PeerConnection connection = manager.connect(request.getCode());

            if (connection == null) {
                return ResponseEntity.badRequest()
                    .body(new ConnectResponse(false, "连接失败 (Connection failed)", null));
            }

            log.info(I18N.get("collaboration.api.connected"), connection.getPeerId());

            PeerInfo peerInfo = new PeerInfo(
                connection.getPeerId(),
                "Peer-" + connection.getPeerId().substring(0, 8),
                "online",
                connection.getConnectTime()
            );

            return ResponseEntity.ok(new ConnectResponse(true, "连接成功 (Connected)", peerInfo));

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.connect_failed"), e);
            return ResponseEntity.internalServerError()
                .body(new ConnectResponse(false, e.getMessage(), null));
        }
    }

    /**
     * 获取协作伙伴列表 (Get peer list)
     *
     * GET /api/collaboration/peers
     *
     * @return 伙伴列表 (Peer list)
     */
    @GetMapping("/peers")
    public ResponseEntity<PeersResponse> getPeers() {
        try {
            String userId = "user-current";
            P2PCollaborationManager manager = getOrCreateManager(userId);

            List<PeerInfo> peers = manager.getConnectedPeers().values().stream()
                .map(conn -> new PeerInfo(
                    conn.getPeerId(),
                    "Peer-" + conn.getPeerId().substring(0, 8),
                    conn.getStatus().toString().toLowerCase(),
                    conn.getConnectTime()
                ))
                .collect(Collectors.toList());

            log.info(I18N.get("collaboration.api.peers_fetched"), peers.size());

            return ResponseEntity.ok(new PeersResponse(true, peers, peers.size()));

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.peers_fetch_failed"), e);
            return ResponseEntity.ok(new PeersResponse(false, Collections.emptyList(), 0));
        }
    }

    /**
     * 断开连接 (Disconnect)
     *
     * DELETE /api/collaboration/peers/{peerId}
     *
     * @param peerId 伙伴ID (Peer ID)
     * @return 断开结果 (Disconnect result)
     */
    @DeleteMapping("/peers/{peerId}")
    public ResponseEntity<ApiResponse> disconnect(@PathVariable String peerId) {
        try {
            String userId = "user-current";
            P2PCollaborationManager manager = getOrCreateManager(userId);

            manager.disconnect(peerId);

            log.info(I18N.get("collaboration.api.disconnected"), peerId);

            return ResponseEntity.ok(new ApiResponse(true, "断开成功 (Disconnected)"));

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.disconnect_failed"), e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * 知识交换 (Exchange knowledge)
     *
     * POST /api/collaboration/exchange
     * Body: { "peerId": "xxx", "knowledge": "..." }
     *
     * @param request 交换请求 (Exchange request)
     * @return 交换结果 (Exchange result)
     */
    @PostMapping("/exchange")
    public ResponseEntity<ApiResponse> exchange(@RequestBody ExchangeRequest request) {
        try {
            String userId = "user-current";
            P2PCollaborationManager manager = getOrCreateManager(userId);

            boolean success = manager.sendKnowledge(request.getPeerId(), request.getKnowledge());

            if (success) {
                log.info(I18N.get("collaboration.api.knowledge_sent"), request.getPeerId());
                return ResponseEntity.ok(new ApiResponse(true, "知识发送成功 (Knowledge sent)"));
            } else {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "知识发送失败 (Knowledge send failed)"));
            }

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.exchange_failed"), e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * 获取交换历史 (Get exchange history)
     *
     * GET /api/collaboration/exchange-history
     *
     * @return 交换历史 (Exchange history)
     */
    @GetMapping("/exchange-history")
    public ResponseEntity<ExchangeHistoryResponse> getExchangeHistory() {
        try {
            // 暂时返回模拟数据 (Return mock data temporarily)
            List<ExchangeRecord> history = Arrays.asList(
                new ExchangeRecord("1", "user-current", "peer-a", "知识A", LocalDateTime.now().minusHours(2)),
                new ExchangeRecord("2", "peer-b", "user-current", "知识B", LocalDateTime.now().minusHours(1))
            );

            log.info(I18N.get("collaboration.api.history_fetched"), history.size());

            return ResponseEntity.ok(new ExchangeHistoryResponse(true, history, history.size()));

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.history_fetch_failed"), e);
            return ResponseEntity.ok(new ExchangeHistoryResponse(false, Collections.emptyList(), 0));
        }
    }

    /**
     * 获取网络拓扑 (Get network topology)
     *
     * GET /api/collaboration/topology
     *
     * @return 网络拓扑 (Network topology)
     */
    @GetMapping("/topology")
    public ResponseEntity<TopologyResponse> getTopology() {
        try {
            // 暂时返回模拟数据 (Return mock data temporarily)
            TopologyData topology = new TopologyData(
                Arrays.asList(
                    new TopologyNode("current", "我", "online"),
                    new TopologyNode("peer-a", "Peer A", "online"),
                    new TopologyNode("peer-b", "Peer B", "online")
                ),
                Arrays.asList(
                    new TopologyEdge("current", "peer-a", "connected"),
                    new TopologyEdge("current", "peer-b", "connected")
                )
            );

            log.info(I18N.get("collaboration.api.topology_fetched"));

            return ResponseEntity.ok(new TopologyResponse(true, topology));

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.topology_fetch_failed"), e);
            return ResponseEntity.internalServerError()
                .body(new TopologyResponse(false, null));
        }
    }

    /**
     * 获取同步状态 (Get sync status)
     *
     * GET /api/collaboration/sync-status
     *
     * @return 同步状态 (Sync status)
     */
    @GetMapping("/sync-status")
    public ResponseEntity<SyncStatusResponse> getSyncStatus() {
        try {
            // 暂时返回模拟数据 (Return mock data temporarily)
            SyncStatus status = new SyncStatus(
                LocalDateTime.now().minusMinutes(5),
                "synced",
                0,
                100,
                100
            );

            log.info(I18N.get("collaboration.api.sync_status_fetched"));

            return ResponseEntity.ok(new SyncStatusResponse(true, status));

        } catch (Exception e) {
            log.error(I18N.get("collaboration.api.sync_status_fetch_failed"), e);
            return ResponseEntity.internalServerError()
                .body(new SyncStatusResponse(false, null));
        }
    }

    // ========== DTO 类 (DTO Classes) ==========

    @Data
    public static class GenerateCodeResponse {
        private final boolean success;
        private final String message;
        private final String code;
        private final String userId;
        private final LocalDateTime expiresAt;
    }

    @Data
    public static class ConnectRequest {
        private String code;
    }

    @Data
    public static class ConnectResponse {
        private final boolean success;
        private final String message;
        private final PeerInfo peer;
    }

    @Data
    public static class PeerInfo {
        private final String id;
        private final String name;
        private final String status;
        private final LocalDateTime connectedAt;
    }

    @Data
    public static class PeersResponse {
        private final boolean success;
        private final List<PeerInfo> peers;
        private final int total;
    }

    @Data
    public static class ExchangeRequest {
        private String peerId;
        private String knowledge;
    }

    @Data
    public static class ExchangeRecord {
        private final String id;
        private final String from;
        private final String to;
        private final String knowledge;
        private final LocalDateTime time;
    }

    @Data
    public static class ExchangeHistoryResponse {
        private final boolean success;
        private final List<ExchangeRecord> history;
        private final int total;
    }

    @Data
    public static class TopologyNode {
        private final String id;
        private final String label;
        private final String status;
    }

    @Data
    public static class TopologyEdge {
        private final String from;
        private final String to;
        private final String status;
    }

    @Data
    public static class TopologyData {
        private final List<TopologyNode> nodes;
        private final List<TopologyEdge> edges;
    }

    @Data
    public static class TopologyResponse {
        private final boolean success;
        private final TopologyData topology;
    }

    @Data
    public static class SyncStatus {
        private final LocalDateTime lastSync;
        private final String status;
        private final int pending;
        private final int synced;
        private final int total;
    }

    @Data
    public static class SyncStatusResponse {
        private final boolean success;
        private final SyncStatus syncStatus;
    }

    @Data
    public static class ApiResponse {
        private final boolean success;
        private final String message;
    }
}

