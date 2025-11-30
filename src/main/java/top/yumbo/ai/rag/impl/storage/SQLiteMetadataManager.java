package top.yumbo.ai.rag.impl.storage;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.model.Document;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * SQLite元数据管理器
 * 使用SQLite数据库存储文档元数据
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class SQLiteMetadataManager implements AutoCloseable {

    private final String dbPath;
    private Connection connection;

    private static final String CREATE_DOCUMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS documents (
                id TEXT PRIMARY KEY,
                title TEXT,
                file_path TEXT NOT NULL,
                file_size INTEGER,
                mime_type TEXT,
                content_hash TEXT NOT NULL,
                category TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                metadata TEXT
            )
            """;

    private static final String CREATE_HASH_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_content_hash ON documents(content_hash)
            """;

    private static final String CREATE_CATEGORY_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_category ON documents(category)
            """;

    public SQLiteMetadataManager(String dbPath) {
        this.dbPath = dbPath;
        initialize();
    }

    /**
     * 初始化数据库
     */
    private void initialize() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            // 创建表
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(CREATE_DOCUMENTS_TABLE);
                stmt.execute(CREATE_HASH_INDEX);
                stmt.execute(CREATE_CATEGORY_INDEX);
            }

            log.info("SQLite metadata manager initialized: {}", dbPath);
        } catch (SQLException e) {
            log.error("Failed to initialize SQLite database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * 保存文档元数据
     */
    public void save(Document document) {
        String sql = """
                INSERT OR REPLACE INTO documents 
                (id, title, file_path, file_size, mime_type, content_hash, category, 
                 created_at, updated_at, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, document.getId());
            pstmt.setString(2, document.getTitle());
            pstmt.setString(3, document.getFilePath());
            pstmt.setObject(4, document.getFileSize());
            pstmt.setString(5, document.getMimeType());
            pstmt.setString(6, document.getContentHash());
            pstmt.setString(7, document.getCategory());
            pstmt.setLong(8, document.getCreatedAt() != null ? document.getCreatedAt().toEpochMilli() : System.currentTimeMillis());
            pstmt.setLong(9, document.getUpdatedAt() != null ? document.getUpdatedAt().toEpochMilli() : System.currentTimeMillis());
            pstmt.setString(10, JSON.toJSONString(document.getMetadata()));

            pstmt.executeUpdate();
            log.debug("Document metadata saved: {}", document.getId());
        } catch (SQLException e) {
            log.error("Failed to save document metadata: {}", document.getId(), e);
            throw new RuntimeException("Failed to save document metadata", e);
        }
    }

    /**
     * 获取文档元数据
     */
    public Document get(String id) {
        String sql = "SELECT * FROM documents WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDocument(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get document metadata: {}", id, e);
            throw new RuntimeException("Failed to get document metadata", e);
        }

        return null;
    }

    /**
     * 删除文档元数据
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM documents WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();

            log.debug("Document metadata deleted: {}, affected: {}", id, affected);
            return affected > 0;
        } catch (SQLException e) {
            log.error("Failed to delete document metadata: {}", id, e);
            throw new RuntimeException("Failed to delete document metadata", e);
        }
    }

    /**
     * 检查文档是否存在
     */
    public boolean exists(String id) {
        String sql = "SELECT COUNT(*) FROM documents WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.error("Failed to check document existence: {}", id, e);
            throw new RuntimeException("Failed to check document existence", e);
        }

        return false;
    }

    /**
     * 根据哈希值查找文档
     */
    public List<Document> findByHash(String hash) {
        String sql = "SELECT * FROM documents WHERE content_hash = ?";
        List<Document> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hash);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToDocument(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find documents by hash: {}", hash, e);
            throw new RuntimeException("Failed to find documents by hash", e);
        }

        return results;
    }

    /**
     * 列出所有文档
     */
    public List<Document> listAll() {
        String sql = "SELECT * FROM documents ORDER BY created_at DESC";
        List<Document> results = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                results.add(mapResultSetToDocument(rs));
            }
        } catch (SQLException e) {
            log.error("Failed to list all documents", e);
            throw new RuntimeException("Failed to list all documents", e);
        }

        return results;
    }

    /**
     * 获取文档总数
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM documents";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Failed to count documents", e);
            throw new RuntimeException("Failed to count documents", e);
        }

        return 0;
    }

    /**
     * 获取所有文档ID
     */
    public List<String> getAllDocumentIds() {
        String sql = "SELECT id FROM documents";
        List<String> ids = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ids.add(rs.getString("id"));
            }

            log.debug("Retrieved {} document IDs", ids.size());
        } catch (SQLException e) {
            log.error("Failed to get all document IDs", e);
            throw new RuntimeException("Failed to get all document IDs", e);
        }

        return ids;
    }

    /**
     * 清空所有文档
     */
    public void clear() {
        String sql = "DELETE FROM documents";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            log.info("All document metadata cleared");
        } catch (SQLException e) {
            log.error("Failed to clear documents", e);
            throw new RuntimeException("Failed to clear documents", e);
        }
    }

    /**
     * 将ResultSet映射为Document对象
     */
    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        String metadataJson = rs.getString("metadata");
        Map<String, Object> metadata = metadataJson != null ?
                JSON.parseObject(metadataJson, new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() {}) : new HashMap<>();

        return Document.builder()
                .id(rs.getString("id"))
                .title(rs.getString("title"))
                .filePath(rs.getString("file_path"))
                .fileSize(rs.getObject("file_size", Long.class))
                .mimeType(rs.getString("mime_type"))
                .contentHash(rs.getString("content_hash"))
                .category(rs.getString("category"))
                .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
                .updatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")))
                .metadata(metadata)
                .build();
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                log.info("SQLite connection closed");
            } catch (SQLException e) {
                log.error("Failed to close SQLite connection", e);
            }
        }
    }
}
