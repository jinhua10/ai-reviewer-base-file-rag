package top.yumbo.ai.rag.impl.index;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.core.IndexEngine;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.model.ScoredDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Lucene索引引擎实现 (Lucene index engine implementation)
 * 使用Apache Lucene进行全文索引和搜索 (Uses Apache Lucene for full-text indexing and search)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class LuceneIndexEngine implements IndexEngine {

    private final Directory directory;
    private final Analyzer analyzer;
    private final IndexWriter writer;
    private IndexReader reader;
    private IndexSearcher searcher;

    // 字段名常量
    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_MIME_TYPE = "mimeType";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_FILE_PATH = "filePath";
    private static final String FIELD_CONTENT_HASH = "contentHash";

    public LuceneIndexEngine(RAGConfiguration.IndexConfig config, String basePath) {
        try {
            // 初始化目录
            String indexPath = Paths.get(basePath, "index", "lucene-index").toString();
            Path indexDir = Paths.get(indexPath);

            // 强制删除锁文件（在打开目录前）
            Path lockFile = indexDir.resolve("write.lock");
            if (Files.exists(lockFile)) {
                try {
                    Files.delete(lockFile);
                    log.warn("Removed stale lock file: {}", lockFile);
                } catch (IOException e) {
                    log.warn("Failed to remove stale lock file: {}", lockFile);
                }
            }

            // Use FSDirectory.open to let Lucene choose the best implementation for the platform
            this.directory = FSDirectory.open(indexDir);


            // 初始化分析器
            this.analyzer = new StandardAnalyzer();

            // 配置IndexWriter
            IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
            writerConfig.setRAMBufferSizeMB(config.getRamBufferSizeMB());
            writerConfig.setMaxBufferedDocs(config.getMaxBufferedDocs());
            writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            // 设置合并策略
            TieredMergePolicy mergePolicy = new TieredMergePolicy();
            mergePolicy.setMaxMergedSegmentMB(512);
            writerConfig.setMergePolicy(mergePolicy);

            this.writer = new IndexWriter(directory, writerConfig);

            // 初始化Reader和Searcher
            refreshReader();

            log.info("LuceneIndexEngine initialized at: {}", indexPath);

        } catch (IOException e) {
            log.error("Failed to initialize LuceneIndexEngine", e);
            throw new RuntimeException("Failed to initialize index engine", e);
        }
    }

    @Override
    public void indexDocument(top.yumbo.ai.rag.model.Document document) {
        try {
            org.apache.lucene.document.Document luceneDoc = convertToLuceneDocument(document);

            // 使用updateDocument实现upsert语义
            Term idTerm = new Term(FIELD_ID, document.getId());
            writer.updateDocument(idTerm, luceneDoc);

            log.debug("Document indexed: {}", document.getId());

        } catch (IOException e) {
            log.error("Failed to index document: {}", document.getId(), e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    @Override
    public void indexBatch(Iterable<top.yumbo.ai.rag.model.Document> documents) {
        try {
            for (top.yumbo.ai.rag.model.Document document : documents) {
                org.apache.lucene.document.Document luceneDoc = convertToLuceneDocument(document);
                Term idTerm = new Term(FIELD_ID, document.getId());
                writer.updateDocument(idTerm, luceneDoc);
            }

            writer.commit();
            refreshReader();

            log.info("Batch indexing completed");

        } catch (IOException e) {
            log.error("Failed to index batch", e);
            throw new RuntimeException("Failed to index batch", e);
        }
    }

    @Override
    public void updateIndex(String docId, top.yumbo.ai.rag.model.Document document) {
        indexDocument(document);
    }

    @Override
    public void deleteFromIndex(String docId) {
        try {
            Term idTerm = new Term(FIELD_ID, docId);
            writer.deleteDocuments(idTerm);

            log.debug("Document deleted from index: {}", docId);

        } catch (IOException e) {
            log.error("Failed to delete document from index: {}", docId, e);
            throw new RuntimeException("Failed to delete from index", e);
        }
    }

    @Override
    public SearchResult search(Query query) {
        long startTime = System.currentTimeMillis();

        try {
            // 刷新searcher以获取最新数据
            refreshReader();

            // 构建Lucene查询
            org.apache.lucene.search.Query luceneQuery = buildLuceneQuery(query);

            // 执行搜索
            int totalToFetch = query.getOffset() + query.getLimit();
            TopDocs topDocs = searcher.search(luceneQuery, totalToFetch);

            // 构建结果
            SearchResult result = SearchResult.builder()
                    .query(query.getQueryText())
                    .totalHits(topDocs.totalHits.value)
                    .documents(new ArrayList<>())
                    .build();

            // 处理分页
            int start = query.getOffset();
            int end = Math.min(start + query.getLimit(), topDocs.scoreDocs.length);

            for (int i = start; i < end; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                org.apache.lucene.document.Document luceneDoc = searcher.storedFields().document(scoreDoc.doc);

                // 转换为我们的Document模型（仅包含元数据，不包含content）
                top.yumbo.ai.rag.model.Document doc = convertFromLuceneDocument(luceneDoc);

                // 注意：索引中只存储元数据，不存储 content
                // content 由 LocalFileRAG 从 StorageEngine 加载
                log.trace("Document {} retrieved from index (metadata only, no content)", doc.getId());

                // 创建带评分的文档
                ScoredDocument scoredDoc = ScoredDocument.builder()
                        .document(doc)
                        .score(scoreDoc.score)
                        .build();

                result.addScoredDocument(scoredDoc);
            }

            result.setQueryTimeMs(System.currentTimeMillis() - startTime);
            result.setHasMore(end < topDocs.totalHits.value);
            result.setPage(query.getOffset() / query.getLimit());
            result.setPageSize(query.getLimit());

            log.debug("Search completed: query='{}', hits={}, time={}ms",
                    query.getQueryText(), result.getTotalHits(), result.getQueryTimeMs());

            return result;

        } catch (Exception e) {
            log.error("Search failed: {}", query.getQueryText(), e);
            throw new RuntimeException("Search failed", e);
        }
    }

    @Override
    public void optimize() {
        try {
            log.info("Optimizing index...");
            writer.forceMerge(1);
            writer.commit();
            refreshReader();
            log.info("Index optimization completed");
        } catch (IOException e) {
            log.error("Failed to optimize index", e);
            throw new RuntimeException("Failed to optimize index", e);
        }
    }

    @Override
    public void commit() {
        try {
            if (writer != null && writer.isOpen()) {
                writer.commit();
                refreshReader();
                log.debug("Index committed");
            } else {
                log.debug("IndexWriter is not open, skipping commit");
            }
        } catch (org.apache.lucene.store.AlreadyClosedException e) {
            log.debug("IndexWriter already closed, skipping commit");
        } catch (IOException e) {
            log.error("Failed to commit index", e);
            throw new RuntimeException("Failed to commit index", e);
        }
    }

    @Override
    public long getDocumentCount() {
        try {
            refreshReader();
            return reader.numDocs();
        } catch (IOException e) {
            log.error("Failed to get document count", e);
            return 0;
        }
    }

    @Override
    public void close() {
        try {
            if (writer != null && writer.isOpen()) {
                writer.commit();
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (directory != null) {
                directory.close();
            }
            log.info("LuceneIndexEngine closed");
        } catch (org.apache.lucene.store.AlreadyClosedException e) {
            log.debug("IndexWriter already closed, skipping");
        } catch (IOException e) {
            log.error("Failed to close LuceneIndexEngine", e);
        }
    }

    @Override
    public void rebuild() {
        try {
            writer.deleteAll();
            writer.commit();
            refreshReader();
            log.info("Index rebuilt (cleared)");
        } catch (IOException e) {
            log.error("Failed to rebuild index", e);
            throw new RuntimeException("Failed to rebuild index", e);
        }
    }

    /**
     * 刷新IndexReader和IndexSearcher
     */
    private void refreshReader() throws IOException {
        if (reader == null) {
            reader = DirectoryReader.open(writer);
        } else {
            DirectoryReader newReader = DirectoryReader.openIfChanged((DirectoryReader) reader, writer);
            if (newReader != null) {
                reader.close();
                reader = newReader;
            }
        }
        searcher = new IndexSearcher(reader);
    }

    /**
     * 构建Lucene查询
     */
    private org.apache.lucene.search.Query buildLuceneQuery(Query query) throws ParseException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // 主查询
        if (query.getQueryText() != null && !query.getQueryText().isEmpty()) {
            // 对查询文本进行转义，防止 Lucene 特殊字符导致解析错误
            String escapedQueryText = escapeLuceneSpecialChars(query.getQueryText());

            // 如果转义后为空，使用 MatchAllDocsQuery
            if (escapedQueryText.trim().isEmpty()) {
                return new MatchAllDocsQuery();
            }

            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    query.getFields(),
                    analyzer
            );
            // 禁用通配符查询的首字符限制
            parser.setAllowLeadingWildcard(false);

            org.apache.lucene.search.Query mainQuery = parser.parse(escapedQueryText);
            builder.add(mainQuery, BooleanClause.Occur.MUST);
        }

        // 过滤条件
        if (query.getFilters() != null && !query.getFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : query.getFilters().entrySet()) {
                TermQuery filterQuery = new TermQuery(new Term(entry.getKey(), entry.getValue()));
                builder.add(filterQuery, BooleanClause.Occur.FILTER);
            }
        }

        BooleanQuery booleanQuery = builder.build();

        // 如果没有任何查询条件，返回匹配所有文档的查询
        if (booleanQuery.clauses().isEmpty()) {
            return new MatchAllDocsQuery();
        }

        return booleanQuery;
    }

    /**
     * 转换为Lucene文档
     */
    private org.apache.lucene.document.Document convertToLuceneDocument(
            top.yumbo.ai.rag.model.Document document) {

        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();

        // ID字段 - 存储且索引
        luceneDoc.add(new StringField(FIELD_ID, document.getId(), Field.Store.YES));

        // 标题字段 - 存储且索引
        if (document.getTitle() != null) {
            luceneDoc.add(new TextField(FIELD_TITLE, document.getTitle(), Field.Store.YES));
        }

        // 内容字段 - 仅索引不存储（节省空间）
        if (document.getContent() != null) {
            luceneDoc.add(new TextField(FIELD_CONTENT, document.getContent(), Field.Store.NO));
        }

        // 分类字段 - 存储且索引
        if (document.getCategory() != null) {
            luceneDoc.add(new StringField(FIELD_CATEGORY, document.getCategory(), Field.Store.YES));
        }

        // MIME类型字段
        if (document.getMimeType() != null) {
            luceneDoc.add(new StringField(FIELD_MIME_TYPE, document.getMimeType(), Field.Store.YES));
        }

        // 文件路径
        if (document.getFilePath() != null) {
            luceneDoc.add(new StoredField(FIELD_FILE_PATH, document.getFilePath()));
        }

        // 内容哈希
        if (document.getContentHash() != null) {
            luceneDoc.add(new StringField(FIELD_CONTENT_HASH, document.getContentHash(), Field.Store.YES));
        }

        // 创建时间 - 用于排序
        if (document.getCreatedAt() != null) {
            luceneDoc.add(new LongPoint(FIELD_CREATED_AT, document.getCreatedAt().toEpochMilli()));
            luceneDoc.add(new StoredField(FIELD_CREATED_AT, document.getCreatedAt().toEpochMilli()));
            luceneDoc.add(new NumericDocValuesField(FIELD_CREATED_AT, document.getCreatedAt().toEpochMilli()));
        }

        return luceneDoc;
    }

    /**
     * 从Lucene文档转换
     */
    private top.yumbo.ai.rag.model.Document convertFromLuceneDocument(
            org.apache.lucene.document.Document luceneDoc) {

        Map<String, Object> metadata = new HashMap<>();

        return top.yumbo.ai.rag.model.Document.builder()
                .id(luceneDoc.get(FIELD_ID))
                .title(luceneDoc.get(FIELD_TITLE))
                .category(luceneDoc.get(FIELD_CATEGORY))
                .mimeType(luceneDoc.get(FIELD_MIME_TYPE))
                .filePath(luceneDoc.get(FIELD_FILE_PATH))
                .contentHash(luceneDoc.get(FIELD_CONTENT_HASH))
                .createdAt(luceneDoc.get(FIELD_CREATED_AT) != null ?
                        Instant.ofEpochMilli(Long.parseLong(luceneDoc.get(FIELD_CREATED_AT))) : null)
                .metadata(metadata)
                .build();
    }

    /**
     * 转义 Lucene 查询字符串中的特殊字符
     * <p>
     * Lucene 特殊字符包括：+ - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
     * 这些字符如果在查询中出现，需要进行转义或移除，否则会导致解析错误
     *
     * @param query 原始查询字符串
     * @return 转义后的查询字符串
     */
    private String escapeLuceneSpecialChars(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }

        // 替换连续的特殊字符模式（如 **、||、&&）
        String result = query
            .replaceAll("\\*+", " ")           // ** 或 *** 替换为空格
            .replaceAll("\\?+", " ")           // ?? 或 ??? 替换为空格
            .replaceAll("\\|\\|", " ")         // || 替换为空格
            .replaceAll("&&", " ")             // && 替换为空格
            .replaceAll("!+", " ")             // ! 替换为空格
            .replaceAll("~\\d*", " ")          // ~ 或 ~2 替换为空格（模糊查询）
            .replaceAll("\\^\\d*\\.?\\d*", " ") // ^ 或 ^2 或 ^2.0 替换为空格（boost）
            .replaceAll("[\\[\\]{}()]", " ")   // 括号替换为空格
            .replaceAll("\"", " ")             // 引号替换为空格
            .replaceAll(":", " ")              // 冒号替换为空格（字段限定符）
            .replaceAll("/", " ")              // 斜杠替换为空格
            .replaceAll("\\\\", " ")           // 反斜杠替换为空格
            .replaceAll("\\s+", " ")           // 多个空格合并为一个
            .trim();

        // 如果结果为空或只剩符号，返回空字符串
        if (result.isEmpty() || result.matches("[\\s\\p{Punct}]+")) {
            return "";
        }

        return result;
    }
}
