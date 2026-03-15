package com.fca.algorithm;

import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;

import java.util.List;

/**
 * Общий интерфейс для алгоритма CbO (Close-by-One).
 * Позволяет использовать разные структуры данных за единым контрактом.
 */
public interface CboAlgorithm {

    /**
     * Вычислить все формальные понятия заданного формального контекста.
     *
    */
    List<FormalConcept> computeConcepts(FormalContext context);

    String getName();
}
