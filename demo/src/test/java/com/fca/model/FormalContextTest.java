package com.fca.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Тесты для FormalContext.
 *
 * Техники проектирования тестов:
 * - Boundary Value Analysis: пустой контекст, 1 объект, 1 атрибут
 * - Equivalence Partitioning: пустой, разреженный, плотный, полный контекст
 * - Branch Testing: покрытие всех ветвей в computeExtent/computeIntent
 * - MCDC: условия в hasRelation и computeClosure
 */
class FormalContextTest {

    static FormalContext sampleContext() {
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

    // ===== Boundary Value Analysis: пустой контекст =====
    @Test
    @DisplayName("BVA: пустой контекст (0 объектов, 0 атрибутов)")
    void emptyContext() {
        FormalContext ctx = new FormalContext(List.of(), List.of(), new boolean[0][0]);
        assertAll("пустой контекст",
                () -> assertEquals(0, ctx.getObjectCount()),
                () -> assertEquals(0, ctx.getAttributeCount()),
                () -> assertTrue(ctx.getObjects().isEmpty()),
                () -> assertTrue(ctx.getAttributes().isEmpty())
        );
    }

    // ===== BVA: один объект, один атрибут =====
    @Test
    @DisplayName("BVA: контекст 1×1 с отношением")
    void singleObjectSingleAttribute_true() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{true}});
        assertTrue(ctx.hasRelation(0, 0));
    }

    @Test
    @DisplayName("BVA: контекст 1×1 без отношения")
    void singleObjectSingleAttribute_false() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{false}});
        assertFalse(ctx.hasRelation(0, 0));
    }

    // ===== Equivalence Partitioning: hasRelation =====
    @Test
    @DisplayName("EP: полный контекст — все отношения true")
    void fullContext() {
        FormalContext ctx = new FormalContext(
                List.of("a", "b"), List.of("x", "y"),
                new boolean[][]{{true, true}, {true, true}});
        assertTrue(ctx.hasRelation(0, 0));
        assertTrue(ctx.hasRelation(1, 1));
    }

    @Test
    @DisplayName("EP: пустая инцидентность — все отношения false")
    void emptyIncidence() {
        FormalContext ctx = new FormalContext(
                List.of("a", "b"), List.of("x", "y"),
                new boolean[][]{{false, false}, {false, false}});
        assertFalse(ctx.hasRelation(0, 0));
        assertFalse(ctx.hasRelation(1, 1));
    }

    @Test
    @DisplayName("EP: разреженный контекст — смешанные отношения")
    void sparseContext() {
        FormalContext ctx = new FormalContext(
                List.of("a", "b"), List.of("x", "y"),
                new boolean[][]{{true, false}, {false, false}});
        assertTrue(ctx.hasRelation(0, 0));
        assertFalse(ctx.hasRelation(0, 1));
    }

    // ===== Тесты computeExtent =====
    @Test
    @DisplayName("Branch: computeExtent — пустое множество атрибутов → все объекты")
    void computeExtent_emptyAttrs_allObjects() {
        FormalContext ctx = sampleContext();
        Set<String> extent = ctx.computeExtent(Set.of());
        assertThat(extent, hasSize(4));
        assertThat(extent, containsInAnyOrder("obj1", "obj2", "obj3", "obj4"));
    }

    @Test
    @DisplayName("Branch: computeExtent — один атрибут")
    void computeExtent_singleAttr() {
        FormalContext ctx = sampleContext();
        Set<String> extent = ctx.computeExtent(Set.of("a"));
        // obj1(a), obj2(a), obj4(a) имеют атрибут a
        assertThat(extent, containsInAnyOrder("obj1", "obj2", "obj4"));
    }

    @Test
    @DisplayName("Branch: computeExtent — несколько атрибутов")
    void computeExtent_multipleAttrs() {
        FormalContext ctx = sampleContext();
        Set<String> extent = ctx.computeExtent(Set.of("a", "b"));
        // obj2 и obj4 имеют и a, и b
        assertThat(extent, containsInAnyOrder("obj2", "obj4"));
    }

    @Test
    @DisplayName("Branch: computeExtent — несуществующий атрибут → пустой экстент")
    void computeExtent_unknownAttr() {
        FormalContext ctx = sampleContext();
        Set<String> extent = ctx.computeExtent(Set.of("z"));
        assertTrue(extent.isEmpty());
    }

    // ===== Тесты computeIntent =====
    @Test
    @DisplayName("Branch: computeIntent — пустое множество объектов → все атрибуты")
    void computeIntent_emptyObjs_allAttrs() {
        FormalContext ctx = sampleContext();
        Set<String> intent = ctx.computeIntent(Set.of());
        assertThat(intent, hasSize(4));
        assertThat(intent, containsInAnyOrder("a", "b", "c", "d"));
    }

    @Test
    @DisplayName("Branch: computeIntent — один объект")
    void computeIntent_singleObj() {
        FormalContext ctx = sampleContext();
        Set<String> intent = ctx.computeIntent(Set.of("obj1"));
        // obj1 имеет a, c
        assertEquals(Set.of("a", "c"), intent);
    }

    @Test
    @DisplayName("Branch: computeIntent — несколько объектов")
    void computeIntent_multipleObjs() {
        FormalContext ctx = sampleContext();
        Set<String> intent = ctx.computeIntent(Set.of("obj1", "obj4"));
        // Общие: a, c
        assertEquals(Set.of("a", "c"), intent);
    }

    @Test
    @DisplayName("Branch: computeIntent — все объекты")
    void computeIntent_allObjs() {
        FormalContext ctx = sampleContext();
        Set<String> intent = ctx.computeIntent(Set.of("obj1", "obj2", "obj3", "obj4"));
        // Ни один атрибут не является общим для всех (проверяем вручную)
        // obj1: a,c; obj2: a,b,d; obj3: b,c; obj4: a,b,c,d
        // a: obj1,obj2,obj4 — obj3 нет → нет
        // b: obj2,obj3,obj4 — obj1 нет → нет
        // c: obj1,obj3,obj4 — obj2 нет → нет
        // d: obj2,obj4 — obj1,obj3 нет → нет
        assertTrue(intent.isEmpty());
    }

    // ===== Тесты computeClosure =====
    @Test
    @DisplayName("MCDC: computeClosure — замыкание {a} = {a}")
    void computeClosure_singleAttr() {
        FormalContext ctx = sampleContext();
        Set<String> closure = ctx.computeClosure(Set.of("a"));
        // Extent({a}) = {obj1, obj2, obj4}
        // Intent({obj1, obj2, obj4}) → общие атрибуты: a (только a у всех)
        assertEquals(Set.of("a"), closure);
    }

    @Test
    @DisplayName("MCDC: computeClosure — замыкание {a,b} расширяется")
    void computeClosure_expansion() {
        FormalContext ctx = sampleContext();
        Set<String> closure = ctx.computeClosure(Set.of("a", "b"));
        // Extent({a,b}) = {obj2, obj4}
        // Intent({obj2, obj4}) = {a, b, d}
        assertEquals(Set.of("a", "b", "d"), closure);
    }

    @Test
    @DisplayName("MCDC: computeClosure — уже замкнутое множество")
    void computeClosure_alreadyClosed() {
        FormalContext ctx = sampleContext();
        Set<String> closure = ctx.computeClosure(Set.of("a", "b", "d"));
        assertEquals(Set.of("a", "b", "d"), closure);
    }

    // ===== Тесты валидации =====
    @Test
    @DisplayName("Negative: null объекты вызывают исключение")
    void nullObjects_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FormalContext(null, List.of("a"), new boolean[0][0]));
    }

    @Test
    @DisplayName("Negative: null атрибуты вызывают исключение")
    void nullAttributes_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FormalContext(List.of("x"), null, new boolean[1][0]));
    }

    @Test
    @DisplayName("Negative: null матрица вызывает исключение")
    void nullIncidence_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FormalContext(List.of("x"), List.of("a"), null));
    }

    @Test
    @DisplayName("Negative: несовпадение размерностей строк")
    void mismatchedRows_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new FormalContext(List.of("x", "y"), List.of("a"),
                        new boolean[][]{{true}}));
        assertThat(ex.getMessage(), containsString("не совпадает"));
    }

    @Test
    @DisplayName("Negative: несовпадение размерностей столбцов")
    void mismatchedColumns_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FormalContext(List.of("x"), List.of("a"),
                        new boolean[][]{{true, false}}));
    }

    // ===== Параметризованный тест computeExtent =====
    @ParameterizedTest(name = "computeExtent({0}) для sample → {1} объектов")
    @CsvSource({"a, 3", "b, 3", "c, 3", "d, 2"})
    void extent_parametrized(String attr, int expectedSize) {
        FormalContext ctx = sampleContext();
        Set<String> extent = ctx.computeExtent(Set.of(attr));
        assertEquals(expectedSize, extent.size());
    }

    // ===== Assumption: тест работает только если контекст не пустой =====
    @Test
    @DisplayName("Assumption: computeExtent выполняется только на непустом контексте")
    void assumption_nonEmptyContext() {
        FormalContext ctx = sampleContext();
        assumeTrue(ctx.getObjectCount() > 0, "Контекст должен содержать объекты");
        Set<String> extent = ctx.computeExtent(Set.of("a"));
        assertFalse(extent.isEmpty());
    }

    @Test
    @DisplayName("toString содержит информацию о контексте")
    void toStringContainsInfo() {
        FormalContext ctx = sampleContext();
        String str = ctx.toString();
        assertNotNull(str);
        assertThat(str, containsString("4 объектов"));
        assertThat(str, containsString("4 атрибутов"));
    }
}
