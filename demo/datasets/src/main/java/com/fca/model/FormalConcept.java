package com.fca.model;

import java.util.*;

/**
 * Формальное понятие — пара (extent, intent), где extent ⊆ G, intent ⊆ M,
 * extent' = intent, intent' = extent.
 */
public class FormalConcept {

    private final Set<String> extent;
    private final Set<String> intent;

    public FormalConcept(Set<String> extent, Set<String> intent) {
        this.extent = Collections.unmodifiableSet(new TreeSet<>(extent));
        this.intent = Collections.unmodifiableSet(new TreeSet<>(intent));
    }

    public Set<String> getExtent() {
        return extent;
    }

    public Set<String> getIntent() {
        return intent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormalConcept that = (FormalConcept) o;
        return extent.equals(that.extent) && intent.equals(that.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extent, intent);
    }

    @Override
    public String toString() {
        return "({" + String.join(", ", extent) + "}, {" + String.join(", ", intent) + "})";
    }
}
