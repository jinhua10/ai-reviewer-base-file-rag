package top.yumbo.ai.rag.ai.engine.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.ai.engine.AIEngine;
import top.yumbo.ai.rag.i18n.I18N;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在线 Ollama 引擎 (Remote Ollama Engine)
 * 连接公司内网 Ollama 服务器
 *
 * 适用场景 (Use Case):
 * - 办公电脑性能不足
 * - 公司内网有 Ollama 服务器
 * - 统一管理和维护
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class RemoteOllamaEngine implements AIEngine {

    private List<ServerConfig> servers = new ArrayList<>();
    private String model = "qwen2.5:7b";
    private AtomicInteger currentServerIndex = new AtomicInteger(0);

    /**
     * 服务器配置 (Server Configuration)
     */
    @Data
    public static class ServerConfig {
        private final String host;
        private final int port;
        private boolean available = true;

        public String getUrl() {
            return String.format("http://%s:%d", host, port);
        }
    }

    // ========== 健康检查 (Health Check) ==========

    @Override
    public boolean healthCheck() {
        // 检查所有服务器 (Check all servers)
        boolean anyAvailable = false;

        for (ServerConfig server : servers) {
            boolean healthy = checkServer(server);
            server.setAvailable(healthy);
            if (healthy) {
                anyAvailable = true;
            }
        }

        return anyAvailable;
    }

    /**
     * 检查单个服务器 (Check single server)
     */
    private boolean checkServer(ServerConfig server) {
        try {
            log.debug(I18N.get("ai.engine.remote.connecting"), server.getUrl());

            URL url = new URL(server.getUrl() + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            boolean healthy = (responseCode == 200);

            if (healthy) {
                log.info(I18N.get("ai.engine.remote.connected"), server.getUrl());
            } else {
                log.warn(I18N.get("ai.engine.remote.connection_failed"), server.getUrl());
            }

            return healthy;

        } catch (Exception e) {
            log.warn(I18N.get("ai.engine.remote.connection_failed"), e.getMessage());
            return false;
        }
    }

    // ========== 负载均衡 (Load Balancing) ==========

    /**
     * 获取可用服务器（轮询）(Get available server - round robin)
     */
    private ServerConfig getAvailableServer() {
        List<ServerConfig> availableServers = servers.stream()
                .filter(ServerConfig::isAvailable)
                .toList();

        if (availableServers.isEmpty()) {
            throw new RuntimeException(I18N.get("ai.engine.remote.connection_failed", "no available servers"));
        }

        // 轮询选择 (Round robin)
        int index = currentServerIndex.getAndIncrement() % availableServers.size();
        ServerConfig selected = availableServers.get(index);

        log.debug(I18N.get("ai.engine.remote.load_balancing"), selected.getUrl());
        return selected;
    }

    // ========== 生成回答 (Generate Response) ==========

    @Override
    public String generate(String prompt, GenerateOptions options) {
        try {
            ServerConfig server = getAvailableServer();
            log.info(I18N.get("ai.engine.local.generating"));

            // TODO: 实现 Ollama API 调用
            // 使用选中的服务器

            // 临时返回占位符 (Temporary placeholder)
            return "在线 Ollama 回答 from " + server.getUrl() + ": " + prompt;

        } catch (Exception e) {
            log.error(I18N.get("ai.engine.remote.connection_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void generateStream(String prompt, GenerateOptions options, StreamCallback callback) {
        try {
            ServerConfig server = getAvailableServer();
            log.info(I18N.get("ai.engine.local.generating"));

            // TODO: 实现 Ollama 流式 API 调用

            // 临时实现 (Temporary implementation)
            String response = generate(prompt, options);
            callback.onToken(response, true);

        } catch (Exception e) {
            log.error(I18N.get("ai.engine.remote.connection_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // ========== 模型信息 (Model Info) ==========

    @Override
    public String getModelInfo() {
        return String.format("Remote Ollama: %s (%d servers)", model, servers.size());
    }

    @Override
    public PerformanceLevel estimatePerformance() {
        // 在线 Ollama 服务器通常性能较好 (Remote servers usually have good performance)
        return PerformanceLevel.HIGH;
    }

    // ========== 配置管理 (Configuration) ==========

    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("servers", servers);
        config.put("model", model);
        return config;
    }

    @Override
    public void updateConfiguration(Map<String, Object> config) {
        if (config.containsKey("model")) {
            this.model = (String) config.get("model");
        }

        if (config.containsKey("servers")) {
            // 更新服务器列表 (Update server list)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> serverConfigs = (List<Map<String, Object>>) config.get("servers");

            servers.clear();
            for (Map<String, Object> serverConfig : serverConfigs) {
                String host = (String) serverConfig.get("host");
                int port = (Integer) serverConfig.getOrDefault("port", 11434);
                servers.add(new ServerConfig(host, port));
            }
        }

        log.info(I18N.get("ai.engine.remote.connected"), getModelInfo());
    }
}

