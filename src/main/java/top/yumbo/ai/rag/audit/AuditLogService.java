package top.yumbo.ai.rag.audit;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import com.alibaba.fastjson2.JSON;

/**
 * 审计日志服务
 */
@Slf4j
public class AuditLogService {
    
    private final String auditLogPath;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public AuditLogService(String auditLogPath) {
        this.auditLogPath = auditLogPath;
        initializeAuditLog();
    }
    
    /**
     * 记录审计事件
     */
    public void log(AuditEvent event) {
        try {
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(System.currentTimeMillis());
            
            String logEntry = JSON.toJSONString(event) + "\n";
            Path logFile = getLogFile();
            
            Files.writeString(logFile, logEntry, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
            
            log.debug("Audit logged: {}", event.getAction());
        } catch (Exception e) {
            log.error("Failed to write audit log", e);
        }
    }
    
    /**
     * 记录文档操作
     */
    public void logDocumentOperation(String userId, String username, 
                                     String action, String documentId, 
                                     boolean success) {
        AuditEvent event = AuditEvent.builder()
            .eventType("DOCUMENT")
            .userId(userId)
            .username(username)
            .action(action)
            .resource("document:" + documentId)
            .success(success)
            .build();
        log(event);
    }
    
    /**
     * 记录搜索操作
     */
    public void logSearchOperation(String userId, String username, 
                                   String query, int results) {
        AuditEvent event = AuditEvent.builder()
            .eventType("SEARCH")
            .userId(userId)
            .username(username)
            .action("SEARCH")
            .resource("query:" + query)
            .details("results:" + results)
            .success(true)
            .build();
        log(event);
    }
    
    /**
     * 记录认证事件
     */
    public void logAuthenticationEvent(String username, String action, 
                                       boolean success, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
            .eventType("AUTH")
            .username(username)
            .action(action)
            .success(success)
            .ipAddress(ipAddress)
            .build();
        log(event);
    }
    
    /**
     * 获取当天日志文件
     */
    private Path getLogFile() {
        String date = LocalDate.now().format(dateFormatter);
        return Paths.get(auditLogPath, "audit-" + date + ".log");
    }
    
    /**
     * 初始化审计日志
     */
    private void initializeAuditLog() {
        try {
            Path path = Paths.get(auditLogPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            log.error("Failed to initialize audit log", e);
        }
    }
}