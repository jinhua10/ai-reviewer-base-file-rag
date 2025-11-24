package top.yumbo.ai.rag.api;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.api.controller.AdminController;
import top.yumbo.ai.rag.api.controller.DocumentController;
import top.yumbo.ai.rag.api.controller.SearchController;
import top.yumbo.ai.rag.api.server.HttpRequestHandler;
import top.yumbo.ai.rag.api.server.NettyHttpServer;
import top.yumbo.ai.rag.config.RAGConfiguration;

@Slf4j
public class ApiServer {
    
    private final LocalFileRAG rag;
    private final NettyHttpServer httpServer;
    private final int port;
    
    public ApiServer(LocalFileRAG rag, int port) {
        this.rag = rag;
        this.port = port;
        
        // 创建控制器
        DocumentController documentController = new DocumentController(rag);
        SearchController searchController = new SearchController(rag);
        AdminController adminController = new AdminController(rag);
        
        // 创建请求处理器
        HttpRequestHandler requestHandler = new HttpRequestHandler(
            documentController,
            searchController,
            adminController
        );
        
        // 创建HTTP服务器
        this.httpServer = new NettyHttpServer(port, requestHandler);
    }
    
    /**
     * 启动API服务器
     */
    public void start() throws Exception {
        httpServer.start();
        log.info("API Server started on port {}", port);
    }
    
    /**
     * 停止API服务器
     */
    public void stop() {
        httpServer.shutdown();
        rag.close();
        log.info("API Server stopped");
    }
    
    /**
     * 主方法 - 示例
     */
    public static void main(String[] args) throws Exception {
        // 创建配置
        RAGConfiguration config = RAGConfiguration.builder()
            .storage(RAGConfiguration.StorageConfig.builder()
                .basePath("./data")
                .build())
            .build();
        
        // 创建RAG实例
        LocalFileRAG rag = LocalFileRAG.builder()
            .configuration(config)
            .build();
        
        // 创建并启动API服务器
        ApiServer server = new ApiServer(rag, 8080);
        server.start();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}