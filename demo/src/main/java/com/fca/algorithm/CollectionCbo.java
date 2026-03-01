package com.fca.algorithm;

import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;

import java.util.*;

/**
 * Реализация алгоритма CbO на основе стандартных коллекций Java (HashSet, ArrayList).
 */
public class CollectionCbo implements CboAlgorithm {

    @Override
    public String getName() {
        return "CbO (Collections)";
    }

    @Override
    public List<FormalConcept> computeConcepts(FormalContext context) {
        int nObj = context.getObjectCount();
        int nAttr = context.getAttributeCount();

        if (nAttr == 0) {
            // Единственное понятие: (G, ∅)
            Set<String> allObjects = new TreeSet<>(context.getObjects());
            return List.of(new FormalConcept(allObjects, Collections.emptySet()));
        }

        // Построить столбцевые множества: для каждого атрибута — множество индексов объектов
        List<Set<Integer>> attrExtents = new ArrayList<>();
        for (int j = 0; j < nAttr; j++) {
            Set<Integer> ext = new HashSet<>();
            for (int i = 0; i < nObj; i++) {
                if (context.hasRelation(i, j)) {
                    ext.add(i);
                }
            }
            attrExtents.add(ext);
        }

        // Начальный экстент — все объекты
        Set<Integer> initExtent = new HashSet<>();
        for (int i = 0; i < nObj; i++) {
            initExtent.add(i);
        }
        // Начальный интент — замыкание всех объектов
        Set<Integer> initIntent = computeIntent(initExtent, context, nAttr);

        List<FormalConcept> result = new ArrayList<>();
        cbo(initExtent, initIntent, 0, nAttr, attrExtents, context, result);
        return result;
    }

    private void cbo(Set<Integer> extent, Set<Integer> intent, int y, int nAttr,
                     List<Set<Integer>> attrExtents, FormalContext context,
                     List<FormalConcept> result) {
        result.add(toConcept(extent, intent, context));

        for (int j = y; j < nAttr; j++) {
            if (!intent.contains(j)) {
                // Новый экстент: пересечение текущего экстента с объектами атрибута j
                Set<Integer> newExtent = new HashSet<>(extent);
                newExtent.retainAll(attrExtents.get(j));

                // Новый интент: замыкание нового экстента
                Set<Integer> newIntent = computeIntent(newExtent, context, nAttr);

                // Проверка каноничности
                if (isCanonical(intent, newIntent, j)) {
                    cbo(newExtent, newIntent, j + 1, nAttr, attrExtents, context, result);
                }
            }
        }
    }

    /**
     * Проверка каноничности: новый интент должен совпадать со старым
     * на всех позициях 0..j-1.
     */
    boolean isCanonical(Set<Integer> oldIntent, Set<Integer> newIntent, int j) {
        for (int i = 0; i < j; i++) {
            if (newIntent.contains(i) != oldIntent.contains(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Вычисление интента (A') для заданного экстента A.
     */
    Set<Integer> computeIntent(Set<Integer> extent, FormalContext context, int nAttr) {
        Set<Integer> intent = new HashSet<>();
        for (int j = 0; j < nAttr; j++) {
            boolean allHave = true;
            for (int i : extent) {
                if (!context.hasRelation(i, j)) {
                    allHave = false;
                    break;
                }
            }
            if (allHave) {
                intent.add(j);
            }
        }
        return intent;
    }

    private FormalConcept toConcept(Set<Integer> extentIdx, Set<Integer> intentIdx,
                                   FormalContext context) {
        Set<String> extent = new TreeSet<>();
        for (int i : extentIdx) {
            extent.add(context.getObjects().get(i));
        }
        Set<String> intent = new TreeSet<>();
        for (int j : intentIdx) {
            intent.add(context.getAttributes().get(j));
        }
        return new FormalConcept(extent, intent);
    }
}
