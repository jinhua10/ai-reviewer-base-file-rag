package top.yumbo.ai.rag.api.controller;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.api.model.ApiResponse;
import top.yumbo.ai.rag.api.model.DocumentRequest;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.util.DocumentUtils;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

@Slf4j
public class DocumentController {
    
    private final LocalFileRAG rag;

    public DocumentController(LocalFileRAG rag) {
        this.rag = rag;
    }
    
    /**
     * 创建文档 / Create document
     */
    public ApiResponse<String> createDocument(String requestBody) {
        try {
            DocumentRequest request = JSON.parseObject(requestBody, DocumentRequest.class);
            
            Document doc = DocumentUtils.fromText(request.getTitle(), request.getContent());
            if (request.getCategory() != null) {
                doc.setCategory(request.getCategory());
            }
            if (request.getMetadata() != null) {
                doc.setMetadata(request.getMetadata());
            }
            
            String docId = rag.index(doc);
            rag.commit();
            
            log.info(LogMessageProvider.getMessage("log.docs.created", docId));
            return ApiResponse.success(LogMessageProvider.getMessage("log.docs.created.success"), docId);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.docs.create.failed", e.getMessage()), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.docs.create.failed", e.getMessage()));
        }
    }
    
    /**
     * 获取文档 / Get document
     */
    public ApiResponse<Document> getDocument(String id) {
        try {
            Document doc = rag.getDocument(id);
            if (doc == null) {
                return ApiResponse.error(LogMessageProvider.getMessage("log.docs.not_found", id));
            }
            return ApiResponse.success(doc);
            
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.docs.get.failed", id), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.docs.get.failed", e.getMessage()));
        }
    }
    
    /**
     * 更新文档 (Update document)
     */
    public ApiResponse<String> updateDocument(String id, String requestBody) {
        try {
            DocumentRequest request = JSON.parseObject(requestBody, DocumentRequest.class);
            
            Document doc = DocumentUtils.fromText(request.getTitle(), request.getContent());
            if (request.getCategory() != null) {
                doc.setCategory(request.getCategory());
            }
            if (request.getMetadata() != null) {
                doc.setMetadata(request.getMetadata());
            }
            
            boolean updated = rag.updateDocument(id, doc);
            if (!updated) {
                return ApiResponse.error(LogMessageProvider.getMessage("log.docs.not_found", id));
            }
            
            rag.commit();
            log.info(LogMessageProvider.getMessage("log.docs.updated", id));
            return ApiResponse.success(LogMessageProvider.getMessage("log.docs.update.success"), id);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.docs.update.failed", id), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.docs.update.failed", e.getMessage()));
        }
    }
    
    /**
     * 删除文档 (Delete document)
     */
    public ApiResponse<String> deleteDocument(String id) {
        try {
            boolean deleted = rag.deleteDocument(id);
            if (!deleted) {
                return ApiResponse.error(LogMessageProvider.getMessage("log.docs.not_found", id));
            }
            
            rag.commit();
            log.info(LogMessageProvider.getMessage("log.docs.deleted", id));
            return ApiResponse.success(LogMessageProvider.getMessage("log.docs.deleted.success"), id);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.docs.delete.failed", id), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.docs.delete.failed", e.getMessage()));
        }
    }
    
    /**
     * 列出文档 (List documents)
     */
    public ApiResponse<Object> listDocuments() {
        try {
            var stats = rag.getStatistics();
            return ApiResponse.success(LogMessageProvider.getMessage("log.docs.total", stats.getDocumentCount()), stats);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.docs.list.failed", e.getMessage()), e);
            return ApiResponse.error(LogMessageProvider.getMessage("log.docs.list.failed", e.getMessage()));
        }
    }
}