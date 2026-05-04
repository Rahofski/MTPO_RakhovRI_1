package com.fca.service;

import com.fca.algorithm.BitSetCbo;
import com.fca.implication.ImplicationGenerator;
import com.fca.model.AnalysisResult;
import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonServiceEpTest {

    private AnalysisResult analyze(FormalContext context) {
        TimingService timingService = new TimingService() {
            private long current;

            @Override
            public long currentTimeNanos() {
                current += 1_000_000L;
                return current;
            }
        };

        ComparisonService service = new ComparisonService(timingService, new ImplicationGenerator());
        return service.runAlgorithm(context, new BitSetCbo());
    }

    private FormalContext emptyContext() {
        return new FormalContext(List.of(), List.of(), new boolean[0][0]);
    }

    private FormalContext allFalse2x2() {
        return new FormalContext(
                List.of("o1", "o2"),
                List.of("a", "b"),
                new boolean[][]{{false, false}, {false, false}}
        );
    }

    private FormalContext allTrue2x2() {
        return new FormalContext(
                List.of("o1", "o2"),
                List.of("a", "b"),
                new boolean[][]{{true, true}, {true, true}}
        );
    }

    private FormalContext sparse2x3() {
        return new FormalContext(
                List.of("o1", "o2"),
                List.of("a", "b", "c"),
                new boolean[][]{{true, false, true}, {false, true, false}}
        );
    }

    private FormalContext mixed4x4() {
        return new FormalContext(
                List.of("obj1", "obj2", "obj3", "obj4"),
                List.of("a", "b", "c", "d"),
                new boolean[][]{
                        {true, false, true, false},
                        {true, true, false, true},
                        {false, true, true, false},
                        {true, true, true, true}
                }
        );
    }

    private FormalContext diagonal2x2() {
        return new FormalContext(
                List.of("o1", "o2"),
                List.of("a", "b"),
                new boolean[][]{{true, false}, {false, true}}
        );
    }

    private FormalContext denseButNotFull2x2() {
        return new FormalContext(
                List.of("o1", "o2"),
                List.of("a", "b"),
                new boolean[][]{{true, true}, {true, false}}
        );
    }

    @Test
    @DisplayName("TC-EP-05: контекст 0×0 даёт 1 понятие и 0 импликаций")
    void emptyContextProducesSingleConceptAndNoImplications() {
        AnalysisResult result = analyze(emptyContext());

        assertAll(
                () -> assertEquals(1, result.getConceptCount()),
                () -> assertEquals(0, result.getImplicationCount())
        );
    }

    @Test
    @DisplayName("TC-EP-06: контекст 2×2 all false содержит понятие с пустым intent")
    void allFalseContextContainsConceptWithEmptyIntent() {
        AnalysisResult result = analyze(allFalse2x2());

        assertAll(
                () -> assertTrue(result.getConceptCount() > 0),
                () -> assertTrue(result.getConcepts().stream().anyMatch(concept -> concept.getIntent().isEmpty()))
        );
    }

    @Test
    @DisplayName("TC-EP-07: контекст 2×2 all true содержит понятие (G, M) и импликации")
    void allTrueContextContainsFullConceptAndImplications() {
        AnalysisResult result = analyze(allTrue2x2());

        assertAll(
                () -> assertEquals(1, result.getConceptCount()),
                () -> assertTrue(result.getConcepts().contains(
                        new FormalConcept(Set.of("o1", "o2"), Set.of("a", "b")))),
                () -> assertTrue(result.getImplicationCount() > 0)
        );
    }

    @Test
    @DisplayName("TC-EP-08: разреженный контекст 2×3 даёт мало понятий и корректные пересечения")
    void sparseContextHasFewConceptsAndCorrectIntersections() {
        FormalContext context = sparse2x3();
        AnalysisResult result = analyze(context);

        assertAll(
                () -> assertEquals(4, result.getConceptCount()),
                () -> assertEquals(Set.of("o1"), context.computeExtent(Set.of("a", "c"))),
                () -> assertEquals(Set.of("a", "c"), context.computeIntent(Set.of("o1")))
        );
    }

    @Test
    @DisplayName("TC-EP-09: mixed 4×4 даёт больше понятий и сохраняет каноничность")
    void mixedContextProducesMoreConceptsAndClosedPairs() {
        FormalContext context = mixed4x4();
        AnalysisResult result = analyze(context);

        assertTrue(result.getConceptCount() > 4);
        assertEquals(result.getConceptCount(), new HashSet<>(result.getConcepts()).size());

        for (FormalConcept concept : result.getConcepts()) {
            assertAll(
                    () -> assertEquals(concept.getIntent(), context.computeIntent(concept.getExtent())),
                    () -> assertEquals(concept.getExtent(), context.computeExtent(concept.getIntent()))
            );
        }
    }

    @Test
    @DisplayName("TC-EP-10: диагональный контекст 2×2 даёт 0 импликаций")
    void diagonalContextHasNoImplications() {
        AnalysisResult result = analyze(diagonal2x2());
        assertEquals(0, result.getImplicationCount());
    }

    @Test
    @DisplayName("TC-EP-15: результат для пустого контекста содержит 1 понятие и 0 импликаций")
    void emptyContextResultHasExpectedCounts() {
        AnalysisResult result = analyze(emptyContext());

        assertAll(
                () -> assertEquals(1, result.getConceptCount()),
                () -> assertEquals(0, result.getImplicationCount())
        );
    }

    @Test
    @DisplayName("TC-EP-16: результат для диагонального контекста содержит 0 импликаций")
    void diagonalContextResultHasZeroImplications() {
        AnalysisResult result = analyze(diagonal2x2());
        assertEquals(0, result.getImplicationCount());
    }

    @Test
    @DisplayName("TC-EP-17: результат для плотного контекста содержит больше 1 понятия и больше 0 импликаций")
    void denseContextResultHasConceptsAndImplications() {
        AnalysisResult result = analyze(denseButNotFull2x2());

        assertAll(
                () -> assertTrue(result.getConceptCount() > 1),
                () -> assertTrue(result.getImplicationCount() > 0)
        );
    }
}