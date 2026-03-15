package com.fca.bdd;

import com.fca.algorithm.BitSetCbo;
import com.fca.algorithm.CboAlgorithm;
import com.fca.algorithm.CollectionCbo;
import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import io.cucumber.java.ru.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConceptComputationSteps {

    private FormalContext context;
    private List<FormalConcept> concepts;

    @Допустим("система анализа формальных понятий инициализирована")
    public void система_инициализирована() {
        // готово
    }

    @Допустим("задан формальный контекст типа {string} размером {int}x{int}")
    public void задан_контекст_типа(String type, int nObj, int nAttr) {
        List<String> objects = new ArrayList<>();
        for (int i = 0; i < nObj; i++) objects.add("obj" + i);
        List<String> attributes = new ArrayList<>();
        for (int j = 0; j < nAttr; j++) attributes.add("attr" + j);

        boolean[][] incidence = new boolean[nObj][nAttr];
        switch (type) {
            case "диагональный":
                for (int i = 0; i < nObj; i++)
                    for (int j = 0; j < nAttr; j++)
                        incidence[i][j] = (i == j);
                break;
            case "полный":
                for (int i = 0; i < nObj; i++)
                    Arrays.fill(incidence[i], true);
                break;
            case "разреженный":
                Random rnd = new Random(42);
                for (int i = 0; i < nObj; i++)
                    for (int j = 0; j < nAttr; j++)
                        incidence[i][j] = rnd.nextDouble() < 0.2;
                break;
        }
        context = new FormalContext(objects, attributes, incidence);
    }

    @Допустим("задан контекст с {int} объектами и {int} атрибутами")
    public void задан_контекст_без_атрибутов(int nObj, int nAttr) {
        List<String> objects = new ArrayList<>();
        for (int i = 0; i < nObj; i++) objects.add("obj" + i);
        List<String> attributes = new ArrayList<>();
        for (int j = 0; j < nAttr; j++) attributes.add("attr" + j);

        boolean[][] incidence = new boolean[nObj][nAttr];
        context = new FormalContext(objects, attributes, incidence);
    }

    @Допустим("задан контекст с {int} объектом и {int} атрибутом с отношением true")
    public void задан_контекст_1x1_true(int nObj, int nAttr) {
        context = new FormalContext(
                List.of("obj0"), List.of("attr0"),
                new boolean[][]{{true}}
        );
    }

    @Когда("я запускаю алгоритм {string}")
    public void запускаю_алгоритм(String algoName) {
        CboAlgorithm algo = createAlgorithm(algoName);
        concepts = algo.computeConcepts(context);
    }

    @Тогда("найдено не менее {int} формальных понятий")
    public void найдено_не_менее_понятий(int min) {
        assertTrue(concepts.size() >= min,
                "Найдено " + concepts.size() + " понятий, ожидалось >= " + min);
    }

    @Тогда("каждое понятие удовлетворяет свойству замкнутости")
    public void понятия_замкнуты() {
        for (FormalConcept c : concepts) {
            Set<String> computedIntent = context.computeIntent(c.getExtent());
            assertEquals(c.getIntent(), computedIntent,
                    "Нарушено замыкание для понятия: " + c);
            Set<String> computedExtent = context.computeExtent(c.getIntent());
            assertEquals(c.getExtent(), computedExtent,
                    "Нарушено extent' = intent для понятия: " + c);
        }
    }

    @Тогда("найдено ровно {int} формальное понятие")
    public void найдено_ровно_понятий(int count) {
        assertEquals(count, concepts.size());
    }

    @Тогда("экстент единственного понятия содержит все объекты")
    public void экстент_содержит_все_объекты() {
        assertEquals(1, concepts.size());
        assertEquals(new TreeSet<>(context.getObjects()), concepts.get(0).getExtent());
    }

    static CboAlgorithm createAlgorithm(String name) {
        return switch (name) {
            case "CbO (BitSet)" -> new BitSetCbo();
            case "CbO (Collections)" -> new CollectionCbo();
            default -> throw new IllegalArgumentException("Неизвестный алгоритм: " + name);
        };
    }
}
