package com.fca.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.fca.model.FormalContext;

import java.util.*;

public class FormalContextFuzzTest {
    @FuzzTest(maxDuration = "60s")
    public void fuzzComputeExtentEmptySet(FuzzedDataProvider data) {
        int nObj = data.consumeInt(1, 15);
        int nAttr = data.consumeInt(1, 15);

        List<String> objects = new ArrayList<>();
        for (int i = 0; i < nObj; i++) objects.add("o" + i);
        List<String> attributes = new ArrayList<>();
        for (int i = 0; i < nAttr; i++) attributes.add("a" + i);

        boolean[][] incidence = new boolean[nObj][nAttr];
        for (int i = 0; i < nObj; i++)
            for (int j = 0; j < nAttr; j++)
                incidence[i][j] = data.consumeBoolean();

        FormalContext ctx = new FormalContext(objects, attributes, incidence);

        Set<String> extent = ctx.computeExtent(Collections.emptySet());
        if (extent.size() != nObj) {
            throw new AssertionError(
                    "BUG: computeExtent(∅) вернул " + extent.size() +
                    " объектов, ожидалось " + nObj +
                    ". По определению FCA: ∅' = G (все объекты).");
        }
    }
}
