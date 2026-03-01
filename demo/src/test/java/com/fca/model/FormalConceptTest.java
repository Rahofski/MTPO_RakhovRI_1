package com.fca.model;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для FormalConcept и Implication.
 */
class FormalConceptTest {

    @Test
    @DisplayName("Создание понятия с корректными параметрами")
    void createConcept() {
        FormalConcept concept = new FormalConcept(Set.of("obj1", "obj2"), Set.of("a", "b"));
        assertAll(
                () -> assertEquals(Set.of("obj1", "obj2"), concept.getExtent()),
                () -> assertEquals(Set.of("a", "b"), concept.getIntent()),
                () -> assertNotNull(concept.toString())
        );
    }

    @Test
    @DisplayName("Равенство двух одинаковых понятий")
    void equalConcepts() {
        FormalConcept c1 = new FormalConcept(Set.of("x", "y"), Set.of("a"));
        FormalConcept c2 = new FormalConcept(Set.of("y", "x"), Set.of("a"));
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    @DisplayName("Неравенство понятий с разными экстентами")
    void differentExtentConcepts() {
        FormalConcept c1 = new FormalConcept(Set.of("x"), Set.of("a"));
        FormalConcept c2 = new FormalConcept(Set.of("y"), Set.of("a"));
        assertNotEquals(c1, c2);
    }

    @Test
    @DisplayName("Неравенство понятий с разными интентами")
    void differentIntentConcepts() {
        FormalConcept c1 = new FormalConcept(Set.of("x"), Set.of("a"));
        FormalConcept c2 = new FormalConcept(Set.of("x"), Set.of("b"));
        assertNotEquals(c1, c2);
    }

    @Test
    @DisplayName("Понятие не равно null")
    void conceptNotEqualToNull() {
        FormalConcept c = new FormalConcept(Set.of("x"), Set.of("a"));
        assertNotEquals(null, c);
    }

    @Test
    @DisplayName("Понятие не равно объекту другого типа")
    void conceptNotEqualToOtherType() {
        FormalConcept c = new FormalConcept(Set.of("x"), Set.of("a"));
        assertNotEquals("string", c);
    }

    @Test
    @DisplayName("Понятие с пустым экстентом и интентом")
    void emptyConcept() {
        FormalConcept c = new FormalConcept(Set.of(), Set.of());
        assertTrue(c.getExtent().isEmpty());
        assertTrue(c.getIntent().isEmpty());
    }

    @Test
    @DisplayName("toString содержит элементы")
    void toStringFormat() {
        FormalConcept c = new FormalConcept(Set.of("obj1"), Set.of("a", "b"));
        String s = c.toString();
        assertThat(s, containsString("obj1"));
        assertThat(s, containsString("a"));
        assertThat(s, containsString("b"));
    }

    // === Тесты Implication ===

    @Test
    @DisplayName("Создание импликации")
    void createImplication() {
        Implication imp = new Implication(Set.of("a"), Set.of("b", "c"));
        assertEquals(Set.of("a"), imp.getPremise());
        assertEquals(Set.of("b", "c"), imp.getConclusion());
    }

    @Test
    @DisplayName("Равенство импликаций")
    void equalImplications() {
        Implication i1 = new Implication(Set.of("a", "b"), Set.of("c"));
        Implication i2 = new Implication(Set.of("b", "a"), Set.of("c"));
        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    @DisplayName("Неравенство импликаций с разными посылками")
    void differentPremiseImplications() {
        Implication i1 = new Implication(Set.of("a"), Set.of("c"));
        Implication i2 = new Implication(Set.of("b"), Set.of("c"));
        assertNotEquals(i1, i2);
    }

    @Test
    @DisplayName("Неравенство импликаций с разными заключениями")
    void differentConclusionImplications() {
        Implication i1 = new Implication(Set.of("a"), Set.of("b"));
        Implication i2 = new Implication(Set.of("a"), Set.of("c"));
        assertNotEquals(i1, i2);
    }

    @Test
    @DisplayName("Импликация не равна null")
    void implicationNotEqualToNull() {
        Implication imp = new Implication(Set.of("a"), Set.of("b"));
        assertNotEquals(null, imp);
    }

    @Test
    @DisplayName("Импликация не равна объекту другого типа")
    void implicationNotEqualToOtherType() {
        Implication imp = new Implication(Set.of("a"), Set.of("b"));
        assertNotEquals("string", imp);
    }

    @Test
    @DisplayName("toString импликации содержит стрелку")
    void implicationToString() {
        Implication imp = new Implication(Set.of("x"), Set.of("y"));
        assertThat(imp.toString(), containsString("→"));
    }
}
