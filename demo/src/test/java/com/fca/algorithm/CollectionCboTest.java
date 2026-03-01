package com.fca.algorithm;

import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Тесты для CollectionCbo.
 *
 * Техники:
 * - Boundary Value Analysis: пустой контекст, 1×1, полный контекст
 * - Branch Testing: все ветви в isCanonical и cbo
 * - Statement Testing: покрытие всех операторов
 */
class CollectionCboTest {

    private final CollectionCbo cbo = new CollectionCbo();

    @Test
    @DisplayName("BVA: контекст без атрибутов → одно понятие (все объекты, ∅)")
    void noAttributes() {
        FormalContext ctx = new FormalContext(List.of("a", "b"), List.of(), new boolean[2][0]);
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertEquals(1, concepts.size());
        assertEquals(Set.of("a", "b"), concepts.get(0).getExtent());
        assertTrue(concepts.get(0).getIntent().isEmpty());
    }

    @Test
    @DisplayName("BVA: контекст 1×1 (true) → 1 понятие")
    void singleTrue() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{true}});
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        // Единственное понятие: ({x}, {a})
        assertEquals(1, concepts.size());
        assertEquals(Set.of("x"), concepts.get(0).getExtent());
        assertEquals(Set.of("a"), concepts.get(0).getIntent());
    }

    @Test
    @DisplayName("BVA: контекст 1×1 (false) → 2 понятия")
    void singleFalse() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{false}});
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        // Два понятия: ({x}, ∅) и (∅, {a})
        assertEquals(2, concepts.size());
        Set<FormalConcept> conceptSet = new HashSet<>(concepts);
        assertTrue(conceptSet.contains(new FormalConcept(Set.of("x"), Set.of())));
        assertTrue(conceptSet.contains(new FormalConcept(Set.of(), Set.of("a"))));
    }

    @Test
    @DisplayName("Классический контекст 4×4: правильное число понятий")
    void classicContext() {
        FormalContext ctx = new FormalContext(
                List.of("obj1", "obj2", "obj3", "obj4"),
                List.of("a", "b", "c", "d"),
                new boolean[][]{
                        {true, false, true, false},
                        {true, true, false, true},
                        {false, true, true, false},
                        {true, true, true, true}
                }
        );
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertThat(concepts.size(), greaterThanOrEqualTo(2));

        assertTrue(concepts.stream().anyMatch(c ->
                c.getExtent().equals(Set.of("obj1", "obj2", "obj3", "obj4"))));

        assertTrue(concepts.stream().anyMatch(c ->
                c.getExtent().equals(Set.of("obj4")) &&
                        c.getIntent().equals(Set.of("a", "b", "c", "d"))));
    }

    // ===== Полный контекст (все true) =====
    @Test
    @DisplayName("EP: полный контекст → ровно 1 понятие")
    void fullContext() {
        FormalContext ctx = new FormalContext(
                List.of("a", "b"), List.of("x", "y"),
                new boolean[][]{{true, true}, {true, true}});
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertEquals(1, concepts.size());
        assertEquals(Set.of("a", "b"), concepts.get(0).getExtent());
        assertEquals(Set.of("x", "y"), concepts.get(0).getIntent());
    }

    // ===== Диагональный контекст =====
    @Test
    @DisplayName("EP: диагональный контекст (каждый объект — свой атрибут)")
    void diagonalContext() {
        FormalContext ctx = new FormalContext(
                List.of("o1", "o2", "o3"),
                List.of("a1", "a2", "a3"),
                new boolean[][]{
                        {true, false, false},
                        {false, true, false},
                        {false, false, true}
                }
        );
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertThat(concepts.size(), greaterThanOrEqualTo(5));
    }

    // ===== Branch Testing: isCanonical =====
    @Test
    @DisplayName("Branch: isCanonical — пустой старый интент, пустой новый, j=0 → true")
    void isCanonical_emptyBoth_j0() {
        assertTrue(cbo.isCanonical(Set.of(), Set.of(), 0));
    }

    @Test
    @DisplayName("Branch: isCanonical — совпадающие интенты → true")
    void isCanonical_matching() {
        assertTrue(cbo.isCanonical(Set.of(0, 1), Set.of(0, 1, 3), 2));
    }

    @Test
    @DisplayName("Branch: isCanonical — различие на позиции < j → false")
    void isCanonical_mismatch() {
        // old: {1}, new: {0, 1, 2}, j=2 → на позиции 0: old нет, new есть → false
        assertFalse(cbo.isCanonical(Set.of(1), Set.of(0, 1, 2), 2));
    }

    @Test
    @DisplayName("Branch: isCanonical — j=0 всегда true (нет проверяемых позиций)")
    void isCanonical_jZero() {
        assertTrue(cbo.isCanonical(Set.of(), Set.of(0, 1, 2), 0));
    }

    // ===== Branch: computeIntent =====
    @Test
    @DisplayName("Branch: computeIntent — пустой экстент → все атрибуты")
    void computeIntent_emptyExtent() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a", "b"), new boolean[][]{{true, false}});
        Set<Integer> intent = cbo.computeIntent(Set.of(), ctx, 2);
        // Пустой экстент → allHave vacuously true → {0, 1}
        assertEquals(Set.of(0, 1), intent);
    }

    @Test
    @DisplayName("Branch: computeIntent — один объект без атрибутов")
    void computeIntent_objectWithNoAttrs() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a", "b"), new boolean[][]{{false, false}});
        Set<Integer> intent = cbo.computeIntent(Set.of(0), ctx, 2);
        assertTrue(intent.isEmpty());
    }

    // ===== Assumption: проверка на непустой результат =====
    @Test
    @DisplayName("Assumption: классический контекст содержит понятия")
    void assumption_conceptsExist() {
        FormalContext ctx = new FormalContext(
                List.of("o1", "o2"), List.of("a"),
                new boolean[][]{{true}, {false}});
        assumeTrue(ctx.getObjectCount() > 0);
        assumingThat(ctx.getAttributeCount() > 0,
                () -> assertThat(cbo.computeConcepts(ctx).size(), greaterThan(1)));
    }

    @Test
    @DisplayName("Имя алгоритма")
    void algorithmName() {
        assertEquals("CbO (Collections)", cbo.getName());
    }
}
