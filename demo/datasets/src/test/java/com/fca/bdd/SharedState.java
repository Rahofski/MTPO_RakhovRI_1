package com.fca.bdd;

import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import com.fca.model.Implication;

import java.util.List;

/**
 * Общее состояние между step-классами Cucumber.
 * Cucumber создаёт один экземпляр на каждый сценарий.
 */
public class SharedState {
    public FormalContext context;
    public List<FormalConcept> concepts;
    public List<Implication> implications;
}
