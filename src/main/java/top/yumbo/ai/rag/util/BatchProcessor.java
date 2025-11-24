package top.yumbo.ai.rag.util;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class BatchProcessor<T> {
    private final int batchSize;
    private final Consumer<List<T>> processor;
    private final List<T> buffer;
    public BatchProcessor(int batchSize, Consumer<List<T>> processor) {
        this.batchSize = batchSize;
        this.processor = processor;
        this.buffer = new ArrayList<>(batchSize);
    }
    public void add(T item) {
        buffer.add(item);
        if (buffer.size() >= batchSize) {
            flush();
        }
    }
    public void flush() {
        if (!buffer.isEmpty()) {
            try {
                processor.accept(new ArrayList<>(buffer));
                buffer.clear();
            } catch (Exception e) {
                log.error("Batch processing failed", e);
            }
        }
    }
    public static <T> void processList(List<T> items, int batchSize, 
                                        Consumer<List<T>> processor) {
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            List<T> batch = items.subList(i, end);
            processor.accept(batch);
        }
    }
    public int getBufferSize() {
        return buffer.size();
    }
}
