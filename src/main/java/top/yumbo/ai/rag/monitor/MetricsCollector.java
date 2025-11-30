package top.yumbo.ai.rag.monitor;
import lombok.Data;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import java.util.concurrent.atomic.LongAdder;
@Data
public class MetricsCollector {
    private final LongAdder httpRequests = new LongAdder();
    private final LongAdder httpErrors = new LongAdder();
    private final LongAdder documentsCreated = new LongAdder();
    private final LongAdder documentsUpdated = new LongAdder();
    private final LongAdder documentsDeleted = new LongAdder();
    private final LongAdder searchRequests = new LongAdder();
    private final LongAdder searchErrors = new LongAdder();
    private final LongAdder loginAttempts = new LongAdder();
    private final LongAdder loginFailures = new LongAdder();
    public void recordHttpRequest(boolean success) {
        httpRequests.increment();
        if (!success) {
            httpErrors.increment();
        }
    }
    public String generateReport() {
        return LogMessageProvider.getMessage("log.monitor.metrics.report",
            httpRequests.sum(), httpErrors.sum(),
            documentsCreated.sum(), documentsUpdated.sum(), documentsDeleted.sum(),
            searchRequests.sum(), searchErrors.sum(),
            loginAttempts.sum(), loginFailures.sum());
    }
}
