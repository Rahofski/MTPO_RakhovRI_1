package com.fca.model;

import java.util.*;

/**
 * Результат анализа формального контекста: понятия, импликации и статистика.
 */
public class AnalysisResult {

    private final List<FormalConcept> concepts;
    private final List<Implication> implications;
    private final int objectCount;
    private final int attributeCount;
    private final long executionTimeNanos;
    private final String algorithmName;

    public AnalysisResult(List<FormalConcept> concepts, List<Implication> implications,
                          int objectCount, int attributeCount,
                          long executionTimeNanos, String algorithmName) {
        this.concepts = Collections.unmodifiableList(new ArrayList<>(concepts));
        this.implications = Collections.unmodifiableList(new ArrayList<>(implications));
        this.objectCount = objectCount;
        this.attributeCount = attributeCount;
        this.executionTimeNanos = executionTimeNanos;
        this.algorithmName = algorithmName;
    }

    public List<FormalConcept> getConcepts() { return concepts; }
    public List<Implication> getImplications() { return implications; }
    public int getObjectCount() { return objectCount; }
    public int getAttributeCount() { return attributeCount; }
    public int getConceptCount() { return concepts.size(); }
    public int getImplicationCount() { return implications.size(); }
    public long getExecutionTimeNanos() { return executionTimeNanos; }
    public String getAlgorithmName() { return algorithmName; }

    public double getExecutionTimeMs() {
        return executionTimeNanos / 1_000_000.0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Результат: ").append(algorithmName).append(" ===\n");
        sb.append("Объектов: ").append(objectCount).append(", Атрибутов: ").append(attributeCount).append("\n");
        sb.append("Найдено понятий: ").append(concepts.size()).append("\n");
        sb.append("Найдено импликаций: ").append(implications.size()).append("\n");
        sb.append(String.format("Время выполнения: %.3f мс\n", getExecutionTimeMs()));
        sb.append("\n--- Формальные понятия ---\n");
        for (int i = 0; i < concepts.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(concepts.get(i)).append("\n");
        }
        sb.append("\n--- Замкнутые импликативные правила ---\n");
        if (implications.isEmpty()) {
            sb.append("  Нетривиальных импликаций не найдено.\n");
        }
        for (int i = 0; i < implications.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(implications.get(i)).append("\n");
        }
        return sb.toString();
    }
}
