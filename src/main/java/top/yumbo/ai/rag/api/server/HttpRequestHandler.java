package top.yumbo.ai.rag.api.server;

import com.alibaba.fastjson2.JSON;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.api.controller.DocumentController;
import top.yumbo.ai.rag.api.controller.SearchController;
import top.yumbo.ai.rag.api.controller.AdminController;
import top.yumbo.ai.rag.api.model.ApiResponse;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private final DocumentController documentController;
    private final SearchController searchController;
    private final AdminController adminController;
    
    public HttpRequestHandler(DocumentController documentController,
                             SearchController searchController,
                             AdminController adminController) {
        this.documentController = documentController;
        this.searchController = searchController;
        this.adminController = adminController;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        HttpMethod method = request.method();
        
        log.debug(LogMessageProvider.getMessage("log.http.received_request", method, uri));

        try {
            Object response = routeRequest(request, uri, method);
            sendJsonResponse(ctx, HttpResponseStatus.OK, response);
            
        } catch (IllegalArgumentException e) {
            log.warn(LogMessageProvider.getMessage("log.http.bad_request", e.getMessage()));
            sendJsonResponse(ctx, HttpResponseStatus.BAD_REQUEST,
                ApiResponse.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.http.processing_error"), e);
            sendJsonResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                ApiResponse.error(LogMessageProvider.getMessage("log.http.internal_server_error")));
        }
    }
    
    private Object routeRequest(FullHttpRequest request, String uri, HttpMethod method) {
        // 文档管理端点 (Document management endpoint)
        if (uri.startsWith("/api/documents")) {
            return routeDocumentRequest(request, uri, method);
        }
        // 搜索端点 (Search endpoint)
        else if (uri.startsWith("/api/search")) {
            return routeSearchRequest(request, uri, method);
        }
        // 管理端点 (Admin endpoint)
        else if (uri.startsWith("/api/admin") || uri.startsWith("/api/health") || uri.startsWith("/api/stats")) {
            return routeAdminRequest(request, uri, method);
        }
        else {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.unknown_endpoint", uri));
        }
    }
    
    private Object routeDocumentRequest(FullHttpRequest request, String uri, HttpMethod method) {
        if (method == HttpMethod.POST && uri.equals("/api/documents")) {
            return documentController.createDocument(getRequestBody(request));
        }
        else if (method == HttpMethod.GET && uri.matches("/api/documents/[^/]+")) {
            String id = extractId(uri);
            return documentController.getDocument(id);
        }
        else if (method == HttpMethod.PUT && uri.matches("/api/documents/[^/]+")) {
            String id = extractId(uri);
            return documentController.updateDocument(id, getRequestBody(request));
        }
        else if (method == HttpMethod.DELETE && uri.matches("/api/documents/[^/]+")) {
            String id = extractId(uri);
            return documentController.deleteDocument(id);
        }
        else if (method == HttpMethod.GET && uri.equals("/api/documents")) {
            return documentController.listDocuments();
        }
        else {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.invalid_document_endpoint"));
        }
    }
    
    private Object routeSearchRequest(FullHttpRequest request, String uri, HttpMethod method) {
        if (method == HttpMethod.POST && uri.equals("/api/search")) {
            return searchController.search(getRequestBody(request));
        }
        else if (method == HttpMethod.POST && uri.equals("/api/search/advanced")) {
            return searchController.advancedSearch(getRequestBody(request));
        }
        else {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.invalid_search_endpoint"));
        }
    }
    
    private Object routeAdminRequest(FullHttpRequest request, String uri, HttpMethod method) {
        if (method == HttpMethod.GET && uri.equals("/api/health")) {
            return adminController.health();
        }
        else if (method == HttpMethod.GET && uri.equals("/api/stats")) {
            return adminController.stats();
        }
        else if (method == HttpMethod.POST && uri.equals("/api/admin/optimize")) {
            return adminController.optimizeIndex();
        }
        else if (method == HttpMethod.POST && uri.equals("/api/admin/cache/clear")) {
            return adminController.clearCache();
        }
        else {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.invalid_admin_endpoint"));
        }
    }
    
    private String getRequestBody(FullHttpRequest request) {
        return request.content().toString(StandardCharsets.UTF_8);
    }
    
    private String extractId(String uri) {
        String[] parts = uri.split("/");
        return parts[parts.length - 1];
    }
    
    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, Object data) {
        String json = JSON.toJSONString(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.wrappedBuffer(bytes)
        );
        
        response.headers()
            .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8")
            .set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(LogMessageProvider.getMessage("log.channel.exception"), cause);
        ctx.close();
    }
}