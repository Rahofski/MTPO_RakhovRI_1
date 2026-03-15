package com.fca.model;

import java.util.*;

/**
 * Замкнутое импликативное правило вида A → B,
 * где A — посылка (premise), B — заключение (conclusion), A ∩ B = ∅.
 */
public class Implication {

    private final Set<String> premise;
    private final Set<String> conclusion;

    public Implication(Set<String> premise, Set<String> conclusion) {
        this.premise = Collections.unmodifiableSet(new TreeSet<>(premise));
        this.conclusion = Collections.unmodifiableSet(new TreeSet<>(conclusion));
    }

    public Set<String> getPremise() {
        return premise;
    }

    public Set<String> getConclusion() {
        return conclusion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Implication that = (Implication) o;
        return premise.equals(that.premise) && conclusion.equals(that.conclusion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(premise, conclusion);
    }

    @Override
    public String toString() {
        return "{" + String.join(", ", premise) + "} → {" + String.join(", ", conclusion) + "}";
    }
}
