package top.yumbo.ai.rag.query;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.query.impl.AdvancedQueryProcessor;
import top.yumbo.ai.rag.util.DocumentUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 查询处理器测试
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
class QueryProcessorTest {

    private LocalFileRAG rag;
    private QueryProcessor queryProcessor;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // 创建临时目录
        tempDir = Files.createTempDirectory("query-test-");

        // 创建配置
        RAGConfiguration config = RAGConfiguration.builder()
                .storage(RAGConfiguration.StorageConfig.builder()
                        .basePath(tempDir.toString())
                        .compression(false)
                        .build())
                .cache(RAGConfiguration.CacheConfig.builder()
                        .enabled(true)
                        .documentCacheSize(100)
                        .queryCacheSize(100)
                        .build())
                .build();

        // 初始化RAG
        rag = LocalFileRAG.builder()
                .configuration(config)
                .build();

        // 创建查询处理器
        queryProcessor = new AdvancedQueryProcessor(
                rag.getIndexEngine(),
                rag.getCacheEngine()
        );

        // 索引测试数据
        indexTestDocuments();
    }

    @AfterEach
    void tearDown() {
        // 清理查询处理器
        if (queryProcessor != null) {
            queryProcessor = null;
        }

        // 关闭 RAG
        if (rag != null) {
            try {
                rag.close();
            } catch (Exception e) {
                System.err.println("Error closing RAG: " + e.getMessage());
            } finally {
                rag = null;
            }
        }

        // 清理临时目录
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                // ignore
                            }
                        });
            } catch (Exception e) {
                // ignore
            } finally {
                tempDir = null;
            }
        }

        // 建议 GC 运行（仅建议，不强制）
        System.gc();
    }

    private void indexTestDocuments() {
        for (int i = 0; i < 20; i++) {
            Document doc = DocumentUtils.fromText(
                    "测试文档 " + i,
                    "这是测试文档" + i + "的内容，包含一些测试关键词。"
            );
            doc.setCategory("测试");
            rag.index(doc);
        }
        rag.commit();
    }

    @Test
    void testBasicQuery() {
        QueryRequest request = QueryRequest.builder()
                .queryText("测试")
                .limit(10)
                .build();

        SearchResult result = queryProcessor.process(request);

        assertNotNull(result);
        assertTrue(result.getTotalHits() > 0);
        assertTrue(result.getDocuments().size() <= 10);
    }

    @Test
    void testPaginatedQuery() {
        QueryRequest request = QueryRequest.builder()
                .queryText("测试")
                .limit(5)
                .offset(0)
                .build();

        PagedResult page1 = queryProcessor.processPaged(request);

        assertNotNull(page1);
        assertEquals(0, page1.getCurrentPage());
        assertEquals(5, page1.getPageSize());
        assertFalse(page1.isHasPrevious());

        // 获取第二页
        request.setOffset(5);
        PagedResult page2 = queryProcessor.processPaged(request);

        assertEquals(1, page2.getCurrentPage());
        assertTrue(page2.isHasPrevious());
    }

    @Test
    void testCaching() {
        QueryRequest request = QueryRequest.builder()
                .queryText("文档")
                .limit(10)
                .build();

        // 第一次查询
        long start1 = System.currentTimeMillis();
        SearchResult result1 = queryProcessor.process(request);
        long time1 = System.currentTimeMillis() - start1;

        // 第二次查询（应该从缓存获取）
        long start2 = System.currentTimeMillis();
        SearchResult result2 = queryProcessor.process(request);
        long time2 = System.currentTimeMillis() - start2;

        // 验证缓存生效
        assertTrue(time2 <= time1, "Cached query should be faster or equal");
        assertEquals(result1.getTotalHits(), result2.getTotalHits());

        // 检查缓存统计
        CacheStatistics stats = queryProcessor.getCacheStatistics();
        assertNotNull(stats);
    }

    @Test
    void testScoreFiltering() {
        QueryRequest request = QueryRequest.builder()
                .queryText("测试")
                .minScore(0.1f)
                .limit(10)
                .build();

        SearchResult result = queryProcessor.process(request);

        // 验证所有结果得分都大于阈值
        result.getScoredDocuments().forEach(scored ->
            assertTrue(scored.getScore() >= 0.1f)
        );
    }

    @Test
    void testCustomSorting() {
        QueryRequest request = QueryRequest.builder()
                .queryText("测试")
                .sortField("title")
                .sortOrder(QueryRequest.SortOrder.ASC)
                .limit(10)
                .build();

        SearchResult result = queryProcessor.process(request);

        assertNotNull(result);
        assertTrue(result.getDocuments().size() > 0);
    }

    @Test
    void testCacheStatistics() {
        // 执行多次查询
        for (int i = 0; i < 5; i++) {
            QueryRequest request = QueryRequest.builder()
                    .queryText("测试" + i)
                    .limit(5)
                    .build();
            queryProcessor.process(request);
        }

        // 重复查询（测试缓存命中）
        QueryRequest request = QueryRequest.builder()
                .queryText("测试0")
                .limit(5)
                .build();
        queryProcessor.process(request);

        CacheStatistics stats = queryProcessor.getCacheStatistics();

        assertNotNull(stats);
        System.out.println("Cache Statistics: " + stats);
    }
}

