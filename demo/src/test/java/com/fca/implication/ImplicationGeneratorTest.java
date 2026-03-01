package com.fca.implication;

import com.fca.algorithm.BitSetCbo;
import com.fca.algorithm.CollectionCbo;
import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import com.fca.model.Implication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Тесты для ImplicationGenerator.
 *
 * Техники:
 * - Boundary Value Analysis: пустой набор понятий, одно понятие, много
 * - Equivalence Partitioning: контексты без импликаций, с импликациями
 * - Branch Testing: покрытие ветвей generate (пустой conclusion, дубликаты)
 * - MCDC: условие conclusion.isEmpty() и processedPremises.contains()
 */
class ImplicationGeneratorTest {

    private final ImplicationGenerator generator = new ImplicationGenerator();
    private final CollectionCbo collectionCbo = new CollectionCbo();
    private final BitSetCbo bitSetCbo = new BitSetCbo();

    // ===== BVA: пустой список понятий =====
    @Test
    @DisplayName("BVA: null → пустой список")
    void nullConcepts() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{true}});
        List<Implication> result = generator.generate(ctx, null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("BVA: пустой список понятий → пустой список")
    void emptyConcepts() {
        FormalContext ctx = new FormalContext(
                List.of("x"), List.of("a"), new boolean[][]{{true}});
        List<Implication> result = generator.generate(ctx, List.of());
        assertTrue(result.isEmpty());
    }

    // ===== EP: полный контекст → только тривиальные импликации (каждый атрибут → все остальные) =====
    @Test
    @DisplayName("EP: полный контекст → все импликации корректны")
    void fullContext_trivialImplications() {
        FormalContext ctx = new FormalContext(
                List.of("a", "b"), List.of("x", "y"),
                new boolean[][]{{true, true}, {true, true}});
        List<FormalConcept> concepts = collectionCbo.computeConcepts(ctx);
        List<Implication> impls = generator.generate(ctx, concepts);
        // Каждая импликация корректна: замыкание посылки покрывает заключение
        for (Implication impl : impls) {
            Set<String> closure = ctx.computeClosure(impl.getPremise());
            assertTrue(closure.containsAll(impl.getConclusion()),
                    "Импликация должна быть корректна: " + impl);
        }
    }

    // ===== EP: контекст с импликациями =====
    @Test
    @DisplayName("EP: контекст с замыканиями → есть импликации")
    void contextWithImplications() {
        // {a, b} → закрывается до {a, b, d}
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
        List<FormalConcept> concepts = collectionCbo.computeConcepts(ctx);
        List<Implication> impls = generator.generate(ctx, concepts);
        assertThat(impls, is(not(empty())));

        // Проверяем, что все импликации корректны
        for (Implication impl : impls) {
            Set<String> premiseAndConclusion = new TreeSet<>(impl.getPremise());
            premiseAndConclusion.addAll(impl.getConclusion());
            Set<String> closure = ctx.computeClosure(impl.getPremise());
            // closure(premise) должен содержать и premise, и conclusion
            assertTrue(closure.containsAll(premiseAndConclusion),
                    "Замыкание посылки должно содержать заключение: " + impl);
        }
    }

    // ===== Сравнение импликаций от обеих реализаций =====
    @Test
    @DisplayName("Импликации от CollectionCbo = импликации от BitSetCbo")
    void implicationsMatchBothImplementations() {
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

        List<FormalConcept> collConcepts = collectionCbo.computeConcepts(ctx);
        List<FormalConcept> bitConcepts = bitSetCbo.computeConcepts(ctx);

        List<Implication> collImpls = generator.generate(ctx, collConcepts);
        List<Implication> bitImpls = generator.generate(ctx, bitConcepts);

        assertEquals(new HashSet<>(collImpls), new HashSet<>(bitImpls),
                "Множества импликаций от обеих реализаций должны совпадать");
    }

    // ===== Branch: premise и conclusion не пересекаются =====
    @Test
    @DisplayName("Branch: premise ∩ conclusion = ∅ для каждой импликации")
    void premiseAndConclusionDisjoint() {
        FormalContext ctx = new FormalContext(
                List.of("o1", "o2", "o3"),
                List.of("a", "b", "c", "d"),
                new boolean[][]{
                        {true, true, false, false},
                        {true, false, true, false},
                        {false, true, true, true}
                }
        );
        List<FormalConcept> concepts = collectionCbo.computeConcepts(ctx);
        List<Implication> impls = generator.generate(ctx, concepts);

        for (Implication impl : impls) {
            Set<String> intersection = new TreeSet<>(impl.getPremise());
            intersection.retainAll(impl.getConclusion());
            assertTrue(intersection.isEmpty(),
                    "Посылка и заключение не должны пересекаться: " + impl);
        }
    }

    // ===== Без дублей =====
    @Test
    @DisplayName("Branch: нет дублирующихся импликаций")
    void noDuplicateImplications() {
        FormalContext ctx = new FormalContext(
                List.of("o1", "o2"),
                List.of("a", "b", "c"),
                new boolean[][]{
                        {true, true, false},
                        {true, false, true}
                }
        );
        List<FormalConcept> concepts = collectionCbo.computeConcepts(ctx);
        List<Implication> impls = generator.generate(ctx, concepts);
        Set<Implication> unique = new HashSet<>(impls);
        assertEquals(impls.size(), unique.size(), "Не должно быть дублей");
    }

    // ===== Параметризованный тест =====    
    static Stream<Arguments> contextsForImplications() {
        return Stream.of(
                Arguments.of("Диагональный 3×3", new FormalContext(
                        List.of("o1", "o2", "o3"),
                        List.of("a", "b", "c"),
                        new boolean[][]{
                                {true, false, false},
                                {false, true, false},
                                {false, false, true}
                        }
                )),
                Arguments.of("Антидиагональный 2×2", new FormalContext(
                        List.of("o1", "o2"),
                        List.of("a", "b"),
                        new boolean[][]{
                                {false, true},
                                {true, false}
                        }
                ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contextsForImplications")
    @DisplayName("Корректность импликаций для разных контекстов")
    void parametrizedImplicationCorrectness(String name, FormalContext ctx) {
        List<FormalConcept> concepts = collectionCbo.computeConcepts(ctx);
        List<Implication> impls = generator.generate(ctx, concepts);

        for (Implication impl : impls) {
            assertFalse(impl.getConclusion().isEmpty(),
                    "Заключение не должно быть пустым");
            assertFalse(impl.getPremise().isEmpty(),
                    "Посылка не должна быть пустой (для непустого контекста)");
        }
    }

    @Test
    @DisplayName("Assumption: импликации генерируются при наличии замыканий")
    void assumption_closureExists() {
        FormalContext ctx = new FormalContext(
                List.of("o1", "o2"),
                List.of("a", "b"),
                new boolean[][]{{true, true}, {true, false}});

        // Замыкание {b} = {a,b} (obj1 имеет оба, obj2 нет b → extent({b})={o1} → intent={a,b})
        Set<String> closure = ctx.computeClosure(Set.of("b"));
        assumeTrue(closure.size() > 1, "Замыкание {b} должно расширяться");

        List<FormalConcept> concepts = collectionCbo.computeConcepts(ctx);
        List<Implication> impls = generator.generate(ctx, concepts);
        assertThat(impls, is(not(empty())));
    }
}
