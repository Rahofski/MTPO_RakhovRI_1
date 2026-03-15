package com.fca.service;

import com.fca.algorithm.CboAlgorithm;
import com.fca.implication.ImplicationGenerator;
import com.fca.model.AnalysisResult;
import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import com.fca.model.Implication;

import java.util.*;

/**
 * Сервис сравнения двух реализаций алгоритма CbO.
 */
public class ComparisonService {

    private final TimingService timingService;
    private final ImplicationGenerator implicationGenerator;

    public ComparisonService(TimingService timingService, ImplicationGenerator implicationGenerator) {
        this.timingService = Objects.requireNonNull(timingService);
        this.implicationGenerator = Objects.requireNonNull(implicationGenerator);
    }

    /**
     * Запустить оба алгоритма на заданном контексте и сравнить результаты.
     */
    public ComparisonResult compare(FormalContext context, CboAlgorithm algo1, CboAlgorithm algo2) {
        AnalysisResult result1 = runAlgorithm(context, algo1);
        AnalysisResult result2 = runAlgorithm(context, algo2);

        boolean conceptsMatch = new HashSet<>(result1.getConcepts())
                .equals(new HashSet<>(result2.getConcepts()));
        boolean implicationsMatch = new HashSet<>(result1.getImplications())
                .equals(new HashSet<>(result2.getImplications()));

        return new ComparisonResult(result1, result2, conceptsMatch, implicationsMatch);
    }

    /**
     * Запуск одного алгоритма с замером времени.
     */
    public AnalysisResult runAlgorithm(FormalContext context, CboAlgorithm algorithm) {
        long startTime = timingService.currentTimeNanos();
        List<FormalConcept> concepts = algorithm.computeConcepts(context);
        long endTime = timingService.currentTimeNanos();
        long elapsed = endTime - startTime;

        List<Implication> implications = implicationGenerator.generate(context, concepts);

        return new AnalysisResult(
                concepts, implications,
                context.getObjectCount(), context.getAttributeCount(),
                elapsed, algorithm.getName()
        );
    }
}
