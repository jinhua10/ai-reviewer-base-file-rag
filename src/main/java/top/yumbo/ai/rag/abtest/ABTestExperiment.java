package top.yumbo.ai.rag.abtest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A/B测试实验 (A/B Test Experiment)
 * 定义一个A/B测试实验的配置和状态
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class ABTestExperiment {

    private String experimentId;
    private String name;
    private String conflictId;
    private List<Variant> variants;
    private List<String> targetUsers;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int sampleSize;
    private ExperimentStatus status;

    public ABTestExperiment() {
        this.variants = new ArrayList<>();
        this.targetUsers = new ArrayList<>();
        this.status = ExperimentStatus.PREPARING;
    }

    public ABTestExperiment(String experimentId, String name, String conflictId) {
        this();
        this.experimentId = experimentId;
        this.name = name;
        this.conflictId = conflictId;
    }

    // Getters and Setters
    public String getExperimentId() { return experimentId; }
    public void setExperimentId(String experimentId) { this.experimentId = experimentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getConflictId() { return conflictId; }
    public void setConflictId(String conflictId) { this.conflictId = conflictId; }

    public List<Variant> getVariants() { return variants; }
    public void setVariants(List<Variant> variants) { this.variants = variants; }
    public void addVariant(Variant variant) { this.variants.add(variant); }

    public List<String> getTargetUsers() { return targetUsers; }
    public void setTargetUsers(List<String> targetUsers) { this.targetUsers = targetUsers; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getSampleSize() { return sampleSize; }
    public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }

    public ExperimentStatus getStatus() { return status; }
    public void setStatus(ExperimentStatus status) { this.status = status; }

    public boolean isActive() {
        return status == ExperimentStatus.RUNNING;
    }

    public enum ExperimentStatus {
        PREPARING,
        RUNNING,
        COMPLETED,
        CANCELLED
    }
}

/**
 * 变体 (Variant)
 */
class Variant {
    private String variantId;
    private String conceptId;
    private double allocation; // 分配比例 (Allocation ratio)
    private int exposureCount;
    private double averageScore;

    public Variant(String variantId, String conceptId, double allocation) {
        this.variantId = variantId;
        this.conceptId = conceptId;
        this.allocation = allocation;
    }

    // Getters and Setters
    public String getVariantId() { return variantId; }
    public String getConceptId() { return conceptId; }
    public double getAllocation() { return allocation; }
    public int getExposureCount() { return exposureCount; }
    public void incrementExposure() { this.exposureCount++; }
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
}

