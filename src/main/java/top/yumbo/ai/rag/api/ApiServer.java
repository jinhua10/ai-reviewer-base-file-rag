package top.yumbo.ai.rag.api;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.api.controller.AdminController;
import top.yumbo.ai.rag.api.controller.DocumentController;
import top.yumbo.ai.rag.api.controller.SearchController;
import top.yumbo.ai.rag.api.server.HttpRequestHandler;
import top.yumbo.ai.rag.api.server.NettyHttpServer;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

@Slf4j
public class ApiServer {
    
    private final LocalFileRAG rag;
    private final NettyHttpServer httpServer;
    private final int port;
    
    public ApiServer(LocalFileRAG rag, int port) {
        this.rag = rag;
        this.port = port;
        
        // 创建控制器 (Create controllers)
        DocumentController documentController = new DocumentController(rag);
        SearchController searchController = new SearchController(rag);
        AdminController adminController = new AdminController(rag);
        
        // 创建请求处理器 (Create request handler)
        HttpRequestHandler requestHandler = new HttpRequestHandler(
            documentController,
            searchController,
            adminController
        );
        
        // 创建HTTP服务器 (Create HTTP server)
        this.httpServer = new NettyHttpServer(port, requestHandler);
    }
    
    /**
     * 启动API服务器 (Start the API server)
     */
    public void start() throws Exception {
        httpServer.start();
        log.info(LogMessageProvider.getMessage("log.api.started", port));
    }
    
    /**
     * 停止API服务器 (Stop the API server)
     */
    public void stop() {
        httpServer.shutdown();
        rag.close();
        log.info(LogMessageProvider.getMessage("log.api.stopped"));
    }
    
    /**
     * 主方法 - 示例 (Main method - example)
     */
    public static void main(String[] args) throws Exception {
        // 创建配置 (Create configuration)
        RAGConfiguration config = RAGConfiguration.builder()
            .storage(RAGConfiguration.StorageConfig.builder()
                .basePath("./data")
                .build())
            .build();
        
        // 创建RAG实例 (Create RAG instance)
        LocalFileRAG rag = LocalFileRAG.builder()
            .configuration(config)
            .build();
        
        // 创建并启动API服务器 (Create and start API server)
        ApiServer server = new ApiServer(rag, 8080);
        server.start();
        
        // 添加关闭钩子 (Add shutdown hook)
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}