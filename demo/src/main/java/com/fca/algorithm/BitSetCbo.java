package com.fca.algorithm;

import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;

import java.util.*;

/**
 * Реализация алгоритма CbO на основе битовых множеств (BitSet).
 * Использует операции побитового И для пересечения, что значительно
 * ускоряет вычисления на больших контекстах.
 */
public class BitSetCbo implements CboAlgorithm {

    @Override
    public String getName() {
        return "CbO (BitSet)";
    }

    @Override
    public List<FormalConcept> computeConcepts(FormalContext context) {
        int nObj = context.getObjectCount();
        int nAttr = context.getAttributeCount();

        if (nAttr == 0) {
            Set<String> allObjects = new TreeSet<>(context.getObjects());
            return List.of(new FormalConcept(allObjects, Collections.emptySet()));
        }

        BitSet[] attrExtents = new BitSet[nAttr];
        for (int j = 0; j < nAttr; j++) {
            attrExtents[j] = new BitSet(nObj);
            for (int i = 0; i < nObj; i++) {
                if (context.hasRelation(i, j)) {
                    attrExtents[j].set(i);
                }
            }
        }

        BitSet[] objIntents = new BitSet[nObj];
        for (int i = 0; i < nObj; i++) {
            objIntents[i] = new BitSet(nAttr);
            for (int j = 0; j < nAttr; j++) {
                if (context.hasRelation(i, j)) {
                    objIntents[i].set(j);
                }
            }
        }

        BitSet extent = new BitSet(nObj);
        extent.set(0, nObj);

        BitSet intent = computeIntentBitSet(extent, objIntents, nAttr);

        List<FormalConcept> result = new ArrayList<>();
        cbo(extent, intent, 0, nAttr, attrExtents, objIntents, context, result);
        return result;
    }

    private void cbo(BitSet extent, BitSet intent, int y, int nAttr,
                     BitSet[] attrExtents, BitSet[] objIntents,
                     FormalContext context, List<FormalConcept> result) {
        result.add(toConcept(extent, intent, context));

        for (int j = y; j < nAttr; j++) {
            if (!intent.get(j)) {
                BitSet newExtent = (BitSet) extent.clone();
                newExtent.and(attrExtents[j]);

                BitSet newIntent = computeIntentBitSet(newExtent, objIntents, nAttr);

                if (isCanonical(intent, newIntent, j)) {
                    cbo(newExtent, newIntent, j + 1, nAttr, attrExtents, objIntents, context, result);
                }
            }
        }
    }

    boolean isCanonical(BitSet oldIntent, BitSet newIntent, int j) {
        for (int i = 0; i < j; i++) {
            if (newIntent.get(i) != oldIntent.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Вычисление интента через побитовое И множеств атрибутов всех объектов экстента.
     */
    BitSet computeIntentBitSet(BitSet extent, BitSet[] objIntents, int nAttr) {
        if (extent.isEmpty()) {
            BitSet all = new BitSet(nAttr);
            all.set(0, nAttr);
            return all;
        }
        BitSet intent = new BitSet(nAttr);
        intent.set(0, nAttr); // начинаем со всех атрибутов
        for (int i = extent.nextSetBit(0); i >= 0; i = extent.nextSetBit(i + 1)) {
            intent.and(objIntents[i]); // пересекаем с атрибутами каждого объекта
        }
        return intent;
    }

    private FormalConcept toConcept(BitSet extentBits, BitSet intentBits,
                                   FormalContext context) {
        Set<String> extent = new TreeSet<>();
        for (int i = extentBits.nextSetBit(0); i >= 0; i = extentBits.nextSetBit(i + 1)) {
            extent.add(context.getObjects().get(i));
        }
        Set<String> intent = new TreeSet<>();
        for (int j = intentBits.nextSetBit(0); j >= 0; j = intentBits.nextSetBit(j + 1)) {
            intent.add(context.getAttributes().get(j));
        }
        return new FormalConcept(extent, intent);
    }
}
