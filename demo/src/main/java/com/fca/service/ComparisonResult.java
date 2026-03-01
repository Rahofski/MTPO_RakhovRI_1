package com.fca.service;

import com.fca.model.AnalysisResult;

import java.util.Objects;

/**
 * Результат сравнения двух реализаций алгоритма CbO.
 */
public class ComparisonResult {

    private final AnalysisResult result1;
    private final AnalysisResult result2;
    private final boolean conceptsMatch;
    private final boolean implicationsMatch;

    public ComparisonResult(AnalysisResult result1, AnalysisResult result2,
                            boolean conceptsMatch, boolean implicationsMatch) {
        this.result1 = Objects.requireNonNull(result1);
        this.result2 = Objects.requireNonNull(result2);
        this.conceptsMatch = conceptsMatch;
        this.implicationsMatch = implicationsMatch;
    }

    public AnalysisResult getResult1() { return result1; }
    public AnalysisResult getResult2() { return result2; }
    public boolean isConceptsMatch() { return conceptsMatch; }
    public boolean isImplicationsMatch() { return implicationsMatch; }

    public boolean isFullyConsistent() {
        return conceptsMatch && implicationsMatch;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════╗\n");
        sb.append("║       Сравнение двух реализаций CbO              ║\n");
        sb.append("╠══════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ %-25s │ %-20s ║\n", "Параметр", result1.getAlgorithmName()));
        sb.append(String.format("║ %-25s │ %-20s ║\n", "", result2.getAlgorithmName()));
        sb.append("╠══════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Понятий: %-16d │ %-20d ║\n",
                result1.getConceptCount(), result2.getConceptCount()));
        sb.append(String.format("║ Импликаций: %-13d │ %-20d ║\n",
                result1.getImplicationCount(), result2.getImplicationCount()));
        sb.append(String.format("║ Время (мс): %-13.3f │ %-20.3f ║\n",
                result1.getExecutionTimeMs(), result2.getExecutionTimeMs()));
        sb.append("╠══════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Понятия совпадают:         %-23s ║\n",
                conceptsMatch ? "ДА ✓" : "НЕТ ✗"));
        sb.append(String.format("║ Импликации совпадают:      %-23s ║\n",
                implicationsMatch ? "ДА ✓" : "НЕТ ✗"));
        sb.append("╠══════════════════════════════════════════════════╣\n");

        double speedup = result1.getExecutionTimeNanos() > 0 && result2.getExecutionTimeNanos() > 0
                ? (double) result1.getExecutionTimeNanos() / result2.getExecutionTimeNanos()
                : 0;
        if (speedup > 1.0) {
            sb.append(String.format("║ BitSet быстрее в %.2f раз                       ║\n", speedup));
        } else if (speedup > 0 && speedup < 1.0) {
            sb.append(String.format("║ Collections быстрее в %.2f раз                   ║\n", 1.0 / speedup));
        } else {
            sb.append("║ Скорости сопоставимы                              ║\n");
        }
        sb.append("╚══════════════════════════════════════════════════╝\n");
        return sb.toString();
    }
}
