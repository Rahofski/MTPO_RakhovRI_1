package com.fca.bdd;

import com.fca.algorithm.CboAlgorithm;
import com.fca.implication.ImplicationGenerator;
import com.fca.model.FormalContext;
import com.fca.model.Implication;
import io.cucumber.java.ru.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ImplicationGenerationSteps {

    private final ImplicationGenerator generator = new ImplicationGenerator();
    private final SharedState shared;
    private List<Implication> implications;
    private final Map<String, Set<Implication>> savedSets = new HashMap<>();

    public ImplicationGenerationSteps(SharedState shared) {
        this.shared = shared;
    }

    @Допустим("существует генератор импликаций")
    public void существует_генератор() {
        assertNotNull(generator);
    }

    @Допустим("задан полный контекст {int}x{int}")
    public void задан_полный_контекст(int nObj, int nAttr) {
        List<String> objects = new ArrayList<>();
        for (int i = 0; i < nObj; i++) objects.add("obj" + i);
        List<String> attributes = new ArrayList<>();
        for (int j = 0; j < nAttr; j++) attributes.add("attr" + j);

        boolean[][] incidence = new boolean[nObj][nAttr];
        for (boolean[] row : incidence) Arrays.fill(row, true);

        shared.context = new FormalContext(objects, attributes, incidence);
    }

    @Когда("я вычисляю понятия алгоритмом {string}")
    public void вычисляю_понятия(String algoName) {
        CboAlgorithm algo = ConceptComputationSteps.createAlgorithm(algoName);
        shared.concepts = algo.computeConcepts(shared.context);
    }

    @Когда("генерирую импликации на основе найденных понятий")
    public void генерирую_импликации() {
        implications = generator.generate(shared.context, shared.concepts);
    }

    @Когда("сохраняю импликации как {string}")
    public void сохраняю_импликации(String name) {
        savedSets.put(name, new HashSet<>(implications));
    }

    @Когда("я генерирую импликации из пустого списка понятий")
    public void генерирую_из_пустого() {
        if (shared.context == null) {
            shared.context = new FormalContext(
                    List.of("obj0"), List.of("attr0"),
                    new boolean[][]{{true}}
            );
        }
        implications = generator.generate(shared.context, Collections.emptyList());
    }

    @Тогда("количество импликаций больше {int}")
    public void количество_больше(int min) {
        assertTrue(implications.size() > min,
                "Импликаций: " + implications.size() + ", ожидалось > " + min);
    }

    @Тогда("количество импликаций равно {int}")
    public void количество_равно(int expected) {
        assertEquals(expected, implications.size());
    }

    @Тогда("каждая импликация имеет непустое заключение")
    public void непустое_заключение() {
        for (Implication imp : implications) {
            assertFalse(imp.getConclusion().isEmpty(),
                    "Пустое заключение у: " + imp);
        }
    }

    @Тогда("посылка и заключение каждой импликации не пересекаются")
    public void посылка_и_заключение_не_пересекаются() {
        for (Implication imp : implications) {
            Set<String> intersection = new TreeSet<>(imp.getPremise());
            intersection.retainAll(imp.getConclusion());
            assertTrue(intersection.isEmpty(),
                    "Пересечение посылки и заключения: " + intersection + " в " + imp);
        }
    }

    @Тогда("{string} и {string} содержат одинаковые импликации")
    public void наборы_одинаковые(String name1, String name2) {
        assertEquals(savedSets.get(name1), savedSets.get(name2));
    }
}
