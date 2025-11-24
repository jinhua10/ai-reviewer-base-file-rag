package top.yumbo.ai.rag.api.controller;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.api.model.ApiResponse;
import top.yumbo.ai.rag.api.model.DocumentRequest;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.util.DocumentUtils;

@Slf4j
public class DocumentController {
    
    private final LocalFileRAG rag;
    
    public DocumentController(LocalFileRAG rag) {
        this.rag = rag;
    }
    
    /**
     * 创建文档
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
            
            log.info("Document created: {}", docId);
            return ApiResponse.success("Document created successfully", docId);
            
        } catch (Exception e) {
            log.error("Failed to create document", e);
            return ApiResponse.error("Failed to create document: " + e.getMessage());
        }
    }
    
    /**
     * 获取文档
     */
    public ApiResponse<Document> getDocument(String id) {
        try {
            Document doc = rag.getDocument(id);
            if (doc == null) {
                return ApiResponse.error("Document not found: " + id);
            }
            return ApiResponse.success(doc);
            
        } catch (Exception e) {
            log.error("Failed to get document: {}", id, e);
            return ApiResponse.error("Failed to get document: " + e.getMessage());
        }
    }
    
    /**
     * 更新文档
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
                return ApiResponse.error("Document not found: " + id);
            }
            
            rag.commit();
            log.info("Document updated: {}", id);
            return ApiResponse.success("Document updated successfully", id);
            
        } catch (Exception e) {
            log.error("Failed to update document: {}", id, e);
            return ApiResponse.error("Failed to update document: " + e.getMessage());
        }
    }
    
    /**
     * 删除文档
     */
    public ApiResponse<String> deleteDocument(String id) {
        try {
            boolean deleted = rag.deleteDocument(id);
            if (!deleted) {
                return ApiResponse.error("Document not found: " + id);
            }
            
            rag.commit();
            log.info("Document deleted: {}", id);
            return ApiResponse.success("Document deleted successfully", id);
            
        } catch (Exception e) {
            log.error("Failed to delete document: {}", id, e);
            return ApiResponse.error("Failed to delete document: " + e.getMessage());
        }
    }
    
    /**
     * 列出文档
     */
    public ApiResponse<Object> listDocuments() {
        try {
            var stats = rag.getStatistics();
            return ApiResponse.success("Total documents: " + stats.getDocumentCount(), stats);
            
        } catch (Exception e) {
            log.error("Failed to list documents", e);
            return ApiResponse.error("Failed to list documents: " + e.getMessage());
        }
    }
}