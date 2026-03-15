package com.fca.experiment;

import com.fca.algorithm.BitSetCbo;
import com.fca.algorithm.CollectionCbo;
import com.fca.implication.ImplicationGenerator;
import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import com.fca.model.Implication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Предварительный эксперимент: итеративный поиск размера контекста,
 * при котором полный цикл обработки (вычисление понятий + генерация
 * импликаций) занимает не менее 2 минут.
 *
 * Запуск:
 *   mvn compile -q
 *   java -cp target/classes com.fca.experiment.PreliminaryExperiment
 */
public class PreliminaryExperiment {

    static final long SEED = 42;
    static final double TARGET_MS = 120_000; // 2 min

    public static void main(String[] args) {
        long globalStart = System.currentTimeMillis();

        warmup();

        System.out.println("================================================================");
        System.out.println("  PREDVARITELNY EKSPERIMENT");
        System.out.println("  Cel: najti razmer konteksta s t(obrabotki) >= 2 min");
        System.out.println("  seed=" + SEED + ", target=" + (long)(TARGET_MS/1000) + "s");
        System.out.println("================================================================");
        System.out.println();

        System.out.println("=== RAZREZHENNYE KONTEKSTY (d = 0.20) ===");
        System.out.println("=== Prodolzhenie s 97x93 (41s) ===");
        System.out.println();
        int[] sparse = iterate(0.2, 97, 93, globalStart);
        int[] dense = new int[]{34, 31}; // already found

        System.out.println();
        System.out.println("================================================================");
        System.out.println("  ITOG:");
        if (dense != null)
            System.out.printf("  Dense  max: %dx%d (d=0.70)%n", dense[0], dense[1]);
        if (sparse != null)
            System.out.printf("  Sparse max: %dx%d (d=0.20)%n", sparse[0], sparse[1]);
        System.out.printf("  Total time: %.1f min%n",
                (System.currentTimeMillis() - globalStart) / 60000.0);
        System.out.println("================================================================");
    }

    static int[] iterate(double density, int startObj, int startAttr, long globalStart) {
        CollectionCbo coll = new CollectionCbo();
        BitSetCbo bs = new BitSetCbo();
        ImplicationGenerator gen = new ImplicationGenerator();

        int nObj = startObj;
        int nAttr = startAttr;

        System.out.printf("  %-9s %7s %9s %12s %12s %10s%n",
                "|G|x|M|", "Conc.", "Implic.", "Coll(ms)", "BS(ms)", "Max(s)");
        System.out.println("  " + "-".repeat(65));

        while (true) {
            // Global timeout: 25 minutes
            if (System.currentTimeMillis() - globalStart > 25 * 60_000) {
                System.out.println("  >>> GLOBAL TIMEOUT 25 min");
                return null;
            }

            FormalContext ctx = makeContext(nObj, nAttr, density);

            long collStart = System.nanoTime();
            List<FormalConcept> cc = coll.computeConcepts(ctx);
            List<Implication> ci = gen.generate(ctx, cc);
            long collEnd = System.nanoTime();
            double collMs = (collEnd - collStart) / 1e6;

            long bsStart = System.nanoTime();
            List<FormalConcept> bc = bs.computeConcepts(ctx);
            gen.generate(ctx, bc);
            long bsEnd = System.nanoTime();
            double bsMs = (bsEnd - bsStart) / 1e6;

            double maxMs = Math.max(collMs, bsMs);

            System.out.printf("  %-9s %7d %9d %12.1f %12.1f %10.2f%n",
                    nObj + "x" + nAttr, cc.size(), ci.size(),
                    collMs, bsMs, maxMs / 1000);
            System.out.flush();

            if (maxMs >= TARGET_MS) {
                System.out.printf("  >>> FOUND: %dx%d => %.1f s (%.1f min), "
                                + "concepts=%d, implications=%d%n",
                        nObj, nAttr, maxMs / 1000, maxMs / 60000,
                        cc.size(), ci.size());
                return new int[]{nObj, nAttr};
            }

            int step;
            if (density >= 0.5) {
                step = maxMs < 100 ? 2 : 1;
            } else {
                if (maxMs < 10) step = 5;
                else if (maxMs < 100) step = 4;
                else if (maxMs < 1000) step = 3;
                else if (maxMs < 10000) step = 2;
                else step = 1;
            }

            nObj += step;
            nAttr += step;
        }
    }

    static void warmup() {
        FormalContext w = makeContext(12, 10, 0.5);
        CollectionCbo c = new CollectionCbo();
        BitSetCbo b = new BitSetCbo();
        ImplicationGenerator g = new ImplicationGenerator();
        for (int i = 0; i < 5; i++) {
            List<FormalConcept> fc = c.computeConcepts(w);
            g.generate(w, fc);
            fc = b.computeConcepts(w);
            g.generate(w, fc);
        }
    }

    static FormalContext makeContext(int nObj, int nAttr, double density) {
        Random rnd = new Random(SEED);
        List<String> objects = new ArrayList<>();
        for (int i = 0; i < nObj; i++) objects.add("o" + i);
        List<String> attributes = new ArrayList<>();
        for (int j = 0; j < nAttr; j++) attributes.add("a" + j);
        boolean[][] incidence = new boolean[nObj][nAttr];
        for (int i = 0; i < nObj; i++)
            for (int j = 0; j < nAttr; j++)
                incidence[i][j] = rnd.nextDouble() < density;
        return new FormalContext(objects, attributes, incidence);
    }
}
