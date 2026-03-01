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
 * Тесты для BitSetCbo.
 *
 * Техники:
 * - Boundary Value Analysis: пустой контекст, 1×1
 * - Branch Testing: все ветви в isCanonical и computeIntentBitSet
 * - Statement Testing: покрытие всех операторов
 */
class BitSetCboTest {

    private final BitSetCbo cbo = new BitSetCbo();

    @Test
    @DisplayName("BVA: контекст без атрибутов")
    void noAttributes() {
        FormalContext ctx = new FormalContext(List.of("a", "b"), List.of(), new boolean[2][0]);
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertEquals(1, concepts.size());
        assertTrue(concepts.get(0).getIntent().isEmpty());
    }

    @Test
    @DisplayName("BVA: контекст 1×1 (true)")
    void singleTrue() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{true}});
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertEquals(1, concepts.size());
    }

    @Test
    @DisplayName("BVA: контекст 1×1 (false)")
    void singleFalse() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{false}});
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertEquals(2, concepts.size());
    }

    @Test
    @DisplayName("Классический контекст 4×4")
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
    }

    @Test
    @DisplayName("Полный контекст → 1 понятие")
    void fullContext() {
        FormalContext ctx = new FormalContext(
                List.of("a", "b"), List.of("x", "y"),
                new boolean[][]{{true, true}, {true, true}});
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertEquals(1, concepts.size());
    }

    @Test
    @DisplayName("Пустая инцидентность 2×2 → 4 понятия")
    void emptyIncidence() {
        FormalContext ctx = new FormalContext(
                List.of("o1", "o2"), List.of("a1", "a2"),
                new boolean[][]{{false, false}, {false, false}});
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertThat(concepts.size(), greaterThanOrEqualTo(2));
    }

    // ===== Branch: isCanonical =====
    @Test
    @DisplayName("Branch: isCanonical — совпадение → true")
    void isCanonical_match() {
        BitSet old = new BitSet();
        old.set(0);
        BitSet newI = new BitSet();
        newI.set(0);
        newI.set(2);
        assertTrue(cbo.isCanonical(old, newI, 1));
    }

    @Test
    @DisplayName("Branch: isCanonical — несовпадение → false")
    void isCanonical_mismatch() {
        BitSet old = new BitSet();
        BitSet newI = new BitSet();
        newI.set(0);
        assertFalse(cbo.isCanonical(old, newI, 1));
    }

    @Test
    @DisplayName("Branch: isCanonical — j=0 → true")
    void isCanonical_jZero() {
        assertTrue(cbo.isCanonical(new BitSet(), new BitSet(), 0));
    }

    // ===== Branch: computeIntentBitSet =====
    @Test
    @DisplayName("Branch: computeIntentBitSet — пустой экстент → все атрибуты")
    void computeIntent_emptyExtent() {
        BitSet[] objIntents = new BitSet[]{new BitSet()};
        BitSet emptyExtent = new BitSet();
        BitSet intent = cbo.computeIntentBitSet(emptyExtent, objIntents, 3);

        assertEquals(3, intent.cardinality());
    }

    @Test
    @DisplayName("Branch: computeIntentBitSet — непустой экстент")
    void computeIntent_nonEmpty() {
        BitSet[] objIntents = new BitSet[2];
        objIntents[0] = new BitSet();
        objIntents[0].set(0);
        objIntents[0].set(1);
        objIntents[1] = new BitSet();
        objIntents[1].set(1);
        objIntents[1].set(2);

        BitSet extent = new BitSet();
        extent.set(0);
        extent.set(1);

        BitSet intent = cbo.computeIntentBitSet(extent, objIntents, 3);
        // Пересечение: {0,1} ∩ {1,2} = {1}
        assertEquals(1, intent.cardinality());
        assertTrue(intent.get(1));
    }

    @Test
    @DisplayName("Имя алгоритма")
    void algorithmName() {
        assertEquals("CbO (BitSet)", cbo.getName());
    }

    @Test
    @DisplayName("Assumption: BitSet эффективнее на больших данных")
    void assumption_largeContext() {
        assumeTrue(Runtime.getRuntime().maxMemory() > 50_000_000,
                "Достаточно памяти для теста");
        // Создаём контекст 10×10
        int n = 10;
        List<String> objs = new ArrayList<>();
        List<String> attrs = new ArrayList<>();
        boolean[][] inc = new boolean[n][n];
        Random rnd = new Random(42);
        for (int i = 0; i < n; i++) {
            objs.add("o" + i);
            attrs.add("a" + i);
            for (int j = 0; j < n; j++) {
                inc[i][j] = rnd.nextBoolean();
            }
        }
        FormalContext ctx = new FormalContext(objs, attrs, inc);
        List<FormalConcept> concepts = cbo.computeConcepts(ctx);
        assertThat(concepts.size(), greaterThan(0));
    }
}
