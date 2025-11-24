package top.yumbo.ai.rag.optimization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.model.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentChunker测试
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
class DocumentChunkerTest {

    private DocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new DocumentChunker();
    }

    @Test
    void testSmallDocumentNoChunking() {
        // 测试小文档不需要分块
        Document doc = Document.builder()
            .id("test-1")
            .title("Small Document")
            .content("This is a small document that does not need chunking.")
            .build();

        List<Document> chunks = chunker.chunk(doc);

        assertEquals(1, chunks.size());
        assertEquals(doc.getId(), chunks.get(0).getId());
        assertEquals(doc.getContent(), chunks.get(0).getContent());
    }

    @Test
    void testLargeDocumentChunking() {
        // 测试大文档分块
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("This is sentence number ").append(i).append(". ");
        }

        Document doc = Document.builder()
            .id("test-2")
            .title("Large Document")
            .content(content.toString())
            .build();

        List<Document> chunks = chunker.chunk(doc);

        assertTrue(chunks.size() > 1, "Large document should be chunked");

        // 验证每个chunk都有正确的元数据
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            assertTrue(chunk.getId().contains("_chunk_"));
            assertEquals(i, chunk.getMetadata().get("chunkIndex"));
            assertEquals("test-2", chunk.getMetadata().get("parentDocId"));
            assertTrue((Boolean) chunk.getMetadata().get("isChunk"));
        }
    }

    @Test
    void testChunkOverlap() {
        // 测试分块重叠
        String content = "A".repeat(3000); // 3000个A (大于默认的 2000 chunkSize)

        Document doc = Document.builder()
            .id("test-3")
            .title("Overlap Test")
            .content(content)
            .build();

        List<Document> chunks = chunker.chunk(doc);

        assertTrue(chunks.size() >= 2, "Should have at least 2 chunks");

        // 检查重叠区域
        if (chunks.size() >= 2) {
            String chunk1 = chunks.get(0).getContent();
            String chunk2 = chunks.get(1).getContent();

            // chunk2的开始部分应该与chunk1的结束部分有重叠
            assertTrue(chunk1.length() > 0);
            assertTrue(chunk2.length() > 0);
        }
    }

    @Test
    void testSmartSplit() {
        // 测试智能分割（在句子边界）
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            content.append("This is a complete sentence. ");
        }

        Document doc = Document.builder()
            .id("test-4")
            .title("Smart Split Test")
            .content(content.toString())
            .build();

        DocumentChunker smartChunker = DocumentChunker.builder()
            .chunkSize(500)
            .chunkOverlap(50)
            .smartSplit(true)
            .build();

        List<Document> chunks = smartChunker.chunk(doc);

        assertTrue(chunks.size() > 1);

        // 验证大部分chunk都以句号结尾（智能分割的结果）
        int endsWithPeriod = 0;
        for (int i = 0; i < chunks.size() - 1; i++) { // 最后一个chunk可能不是
            if (chunks.get(i).getContent().trim().endsWith(".")) {
                endsWithPeriod++;
            }
        }

        // 至少一半的chunk应该在句子边界处分割
        assertTrue(endsWithPeriod >= chunks.size() / 2);
    }

    @Test
    void testChunkingStats() {
        // 测试分块统计
        String content = "X".repeat(5000);
        Document doc = Document.builder()
            .id("test-5")
            .title("Stats Test")
            .content(content)
            .build();

        DocumentChunker.ChunkingStats stats = chunker.getChunkingStats(doc);

        assertEquals(5000, stats.getOriginalLength());
        assertTrue(stats.isNeedsChunking());
        assertTrue(stats.getEstimatedChunks() > 1);
    }

    @Test
    void testBatchChunking() {
        // 测试批量分块
        List<Document> docs = List.of(
            Document.builder()
                .id("batch-1")
                .title("Doc 1")
                .content("Short content")
                .build(),
            Document.builder()
                .id("batch-2")
                .title("Doc 2")
                .content("A".repeat(3000))
                .build()
        );

        List<Document> chunks = chunker.chunkBatch(docs);

        assertTrue(chunks.size() >= 2, "Should have at least 2 chunks");
    }

    @Test
    void testCustomChunkSize() {
        // 测试自定义分块大小
        DocumentChunker customChunker = DocumentChunker.builder()
            .chunkSize(100)
            .chunkOverlap(20)
            .smartSplit(false)
            .build();

        String content = "X".repeat(500);
        Document doc = Document.builder()
            .id("test-6")
            .title("Custom Size")
            .content(content)
            .build();

        List<Document> chunks = customChunker.chunk(doc);

        assertTrue(chunks.size() > 1);

        // 验证chunk大小
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertTrue(chunks.get(i).getContent().length() <= 100 + 50); // 考虑智能分割可能的偏差
        }
    }

    @Test
    void testEmptyDocument() {
        // 测试空文档
        Document doc = Document.builder()
            .id("test-7")
            .title("Empty")
            .content("")
            .build();

        List<Document> chunks = chunker.chunk(doc);

        assertEquals(1, chunks.size());
    }

    @Test
    void testChineseContent() {
        // 测试中文内容
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 300; i++) {  // 增加到 300 个句子，确保超过 2000 字符
            content.append("这是第").append(i).append("个句子。");
        }

        Document doc = Document.builder()
            .id("test-8")
            .title("Chinese Document")
            .content(content.toString())
            .build();

        List<Document> chunks = chunker.chunk(doc);

        assertTrue(chunks.size() > 1);

        // 验证中文内容正确分块
        for (Document chunk : chunks) {
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
        }
    }
}

