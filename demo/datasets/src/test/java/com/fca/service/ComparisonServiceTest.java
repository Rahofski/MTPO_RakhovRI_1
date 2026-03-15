package com.fca.service;

import com.fca.algorithm.CboAlgorithm;
import com.fca.implication.ImplicationGenerator;
import com.fca.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для ComparisonService с использованием мокирования.
 *
 * Виды мокирования:
 * 1. Мок TimingService — контроль замера времени
 * 2. Мок CboAlgorithm — подмена алгоритма для изоляции тестов
 * 3. Мок ImplicationGenerator — подмена генератора импликаций
 *
 * Техники:
 * - Branch Testing: оба алгоритма одинаковы / различаются
 * - Decision Table: conceptsMatch × implicationsMatch
 */
@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock
    private TimingService timingService;

    @Mock
    private ImplicationGenerator implicationGenerator;

    @Mock
    private CboAlgorithm algo1;

    @Mock
    private CboAlgorithm algo2;

    private ComparisonService service;

    private FormalContext sampleContext() {
        return new FormalContext(
                List.of("o1", "o2"), List.of("a", "b"),
                new boolean[][]{{true, false}, {false, true}});
    }

    @BeforeEach
    void setUp() {
        service = new ComparisonService(timingService, implicationGenerator);
    }

    // ===== Мокирование TimingService =====
    @Test
    @DisplayName("Мок TimingService: время корректно замеряется")
    void timingServiceMocked() {
        FormalContext ctx = sampleContext();
        List<FormalConcept> concepts = List.of(
                new FormalConcept(Set.of("o1", "o2"), Set.of()));

        when(timingService.currentTimeNanos())
                .thenReturn(1000L)   // start algo1
                .thenReturn(5000L);  // end algo1

        when(algo1.computeConcepts(ctx)).thenReturn(concepts);
        when(algo1.getName()).thenReturn("MockAlgo");
        when(implicationGenerator.generate(eq(ctx), anyList())).thenReturn(List.of());

        AnalysisResult result = service.runAlgorithm(ctx, algo1);

        assertEquals(4000L, result.getExecutionTimeNanos());
        verify(timingService, times(2)).currentTimeNanos();
    }

    // ===== Мокирование CboAlgorithm =====
    @Test
    @DisplayName("Мок CboAlgorithm: алгоритм вызывается с правильным контекстом")
    void algorithmCalledWithContext() {
        FormalContext ctx = sampleContext();
        List<FormalConcept> concepts = List.of(
                new FormalConcept(Set.of("o1"), Set.of("a")));

        when(algo1.computeConcepts(ctx)).thenReturn(concepts);
        when(algo1.getName()).thenReturn("MockAlgo1");
        when(timingService.currentTimeNanos()).thenReturn(0L);
        when(implicationGenerator.generate(eq(ctx), anyList())).thenReturn(List.of());

        AnalysisResult result = service.runAlgorithm(ctx, algo1);

        verify(algo1).computeConcepts(ctx);
        assertEquals(1, result.getConceptCount());
    }

    // ===== Мокирование ImplicationGenerator =====
    @Test
    @DisplayName("Мок ImplicationGenerator: импликации передаются в результат")
    void implicationGeneratorMocked() {
        FormalContext ctx = sampleContext();
        List<FormalConcept> concepts = List.of(
                new FormalConcept(Set.of("o1"), Set.of("a")));
        List<Implication> impls = List.of(
                new Implication(Set.of("a"), Set.of("b")));

        when(algo1.computeConcepts(ctx)).thenReturn(concepts);
        when(algo1.getName()).thenReturn("MockAlgo");
        when(timingService.currentTimeNanos()).thenReturn(0L);
        when(implicationGenerator.generate(ctx, concepts)).thenReturn(impls);

        AnalysisResult result = service.runAlgorithm(ctx, algo1);

        assertEquals(1, result.getImplicationCount());
        verify(implicationGenerator).generate(ctx, concepts);
    }

    // ===== Тестирование compare =====
    @Test
    @DisplayName("Compare: оба алгоритма дают одинаковые результаты")
    void compare_matching() {
        FormalContext ctx = sampleContext();
        FormalConcept concept = new FormalConcept(Set.of("o1"), Set.of("a"));

        when(algo1.computeConcepts(ctx)).thenReturn(List.of(concept));
        when(algo2.computeConcepts(ctx)).thenReturn(List.of(concept));
        when(algo1.getName()).thenReturn("Algo1");
        when(algo2.getName()).thenReturn("Algo2");
        when(timingService.currentTimeNanos()).thenReturn(0L);
        when(implicationGenerator.generate(eq(ctx), anyList())).thenReturn(List.of());

        ComparisonResult result = service.compare(ctx, algo1, algo2);

        assertTrue(result.isConceptsMatch());
        assertTrue(result.isImplicationsMatch());
        assertTrue(result.isFullyConsistent());
    }

    @Test
    @DisplayName("Compare: алгоритмы дают разные понятия")
    void compare_differentConcepts() {
        FormalContext ctx = sampleContext();

        when(algo1.computeConcepts(ctx)).thenReturn(List.of(
                new FormalConcept(Set.of("o1"), Set.of("a"))));
        when(algo2.computeConcepts(ctx)).thenReturn(List.of(
                new FormalConcept(Set.of("o2"), Set.of("b"))));
        when(algo1.getName()).thenReturn("Algo1");
        when(algo2.getName()).thenReturn("Algo2");
        when(timingService.currentTimeNanos()).thenReturn(0L);
        when(implicationGenerator.generate(eq(ctx), anyList())).thenReturn(List.of());

        ComparisonResult result = service.compare(ctx, algo1, algo2);

        assertFalse(result.isConceptsMatch());
        assertFalse(result.isFullyConsistent());
    }

    @Test
    @DisplayName("Compare: разные импликации")
    void compare_differentImplications() {
        FormalContext ctx = sampleContext();
        FormalConcept concept = new FormalConcept(Set.of("o1"), Set.of("a"));

        when(algo1.computeConcepts(ctx)).thenReturn(List.of(concept));
        when(algo2.computeConcepts(ctx)).thenReturn(List.of(concept));
        when(algo1.getName()).thenReturn("Algo1");
        when(algo2.getName()).thenReturn("Algo2");
        when(timingService.currentTimeNanos()).thenReturn(0L);

        // Первый вызов generate — одни импликации, второй — другие
        when(implicationGenerator.generate(eq(ctx), anyList()))
                .thenReturn(List.of(new Implication(Set.of("a"), Set.of("b"))))
                .thenReturn(List.of());

        ComparisonResult result = service.compare(ctx, algo1, algo2);

        assertTrue(result.isConceptsMatch());
        assertFalse(result.isImplicationsMatch());
        assertFalse(result.isFullyConsistent());
    }

    @Test
    @DisplayName("ComparisonResult.getSummary() содержит информацию")
    void summaryContainsInfo() {
        FormalContext ctx = sampleContext();
        FormalConcept concept = new FormalConcept(Set.of("o1"), Set.of("a"));

        when(algo1.computeConcepts(ctx)).thenReturn(List.of(concept));
        when(algo2.computeConcepts(ctx)).thenReturn(List.of(concept));
        when(algo1.getName()).thenReturn("Algo1");
        when(algo2.getName()).thenReturn("Algo2");
        when(timingService.currentTimeNanos())
                .thenReturn(0L)
                .thenReturn(1_000_000L)
                .thenReturn(2_000_000L)
                .thenReturn(2_500_000L);
        when(implicationGenerator.generate(eq(ctx), anyList())).thenReturn(List.of());

        ComparisonResult result = service.compare(ctx, algo1, algo2);
        String summary = result.getSummary();

        assertNotNull(summary);
        assertThat(summary, containsString("Сравнение"));
    }

    @Test
    @DisplayName("AnalysisResult.toString() содержит статистику")
    void analysisResultToString() {
        AnalysisResult result = new AnalysisResult(
                List.of(new FormalConcept(Set.of("o1"), Set.of("a"))),
                List.of(new Implication(Set.of("a"), Set.of("b"))),
                2, 2, 1_000_000, "TestAlgo");

        String str = result.toString();
        assertAll(
                () -> assertThat(str, containsString("TestAlgo")),
                () -> assertThat(str, containsString("1"))
        );
    }

    @Test
    @DisplayName("AnalysisResult: сообщение при отсутствии импликаций")
    void noImplicationsMessage() {
        AnalysisResult result = new AnalysisResult(
                List.of(), List.of(), 2, 2, 0, "Test");
        String str = result.toString();
        assertThat(str, containsString("Нетривиальных импликаций не найдено"));
    }

    @Test
    @DisplayName("ComparisonResult: getSummary при одинаковой скорости")
    void summaryEqualSpeed() {
        AnalysisResult r1 = new AnalysisResult(List.of(), List.of(), 1, 1, 0, "A1");
        AnalysisResult r2 = new AnalysisResult(List.of(), List.of(), 1, 1, 0, "A2");
        ComparisonResult cr = new ComparisonResult(r1, r2, true, true);
        assertThat(cr.getSummary(), containsString("сопоставимы"));
    }

    @Test
    @DisplayName("ComparisonResult: BitSet быстрее")
    void summaryBitSetFaster() {
        AnalysisResult r1 = new AnalysisResult(List.of(), List.of(), 1, 1, 2000, "Collections");
        AnalysisResult r2 = new AnalysisResult(List.of(), List.of(), 1, 1, 1000, "BitSet");
        ComparisonResult cr = new ComparisonResult(r1, r2, true, true);
        assertThat(cr.getSummary(), containsString("BitSet быстрее"));
    }

    @Test
    @DisplayName("ComparisonResult: Collections быстрее")
    void summaryCollectionsFaster() {
        AnalysisResult r1 = new AnalysisResult(List.of(), List.of(), 1, 1, 500, "Collections");
        AnalysisResult r2 = new AnalysisResult(List.of(), List.of(), 1, 1, 2000, "BitSet");
        ComparisonResult cr = new ComparisonResult(r1, r2, true, true);
        assertThat(cr.getSummary(), containsString("Collections быстрее"));
    }
}
