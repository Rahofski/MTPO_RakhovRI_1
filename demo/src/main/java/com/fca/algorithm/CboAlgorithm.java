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
     * @param context формальный контекст
     * @return список всех формальных понятий
     */
    List<FormalConcept> computeConcepts(FormalContext context);

    /**
     * Название реализации алгоритма.
     */
    String getName();
}
