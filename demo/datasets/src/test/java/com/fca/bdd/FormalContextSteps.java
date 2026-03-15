package com.fca.bdd;

import com.fca.model.FormalContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FormalContextSteps {

    private final SharedState shared;
    private Set<String> attrSet;
    private Set<String> resultSet;

    public FormalContextSteps(SharedState shared) {
        this.shared = shared;
    }

    @Допустим("задан формальный контекст с таблицей инцидентности:")
    public void задан_контекст_с_таблицей(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        List<String> headers = new ArrayList<>(table.row(0));
        headers.remove(0);
        List<String> attributes = new ArrayList<>(headers);

        List<String> objects = new ArrayList<>();
        boolean[][] incidence = new boolean[rows.size()][attributes.size()];

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            objects.add(row.get("объект"));
            for (int j = 0; j < attributes.size(); j++) {
                String val = row.get(attributes.get(j));
                incidence[i][j] = "да".equalsIgnoreCase(val) || "true".equalsIgnoreCase(val)
                        || "+".equals(val) || "1".equals(val);
            }
        }

        shared.context = new FormalContext(objects, attributes, incidence);
    }

    // ── Императивный стиль ──

    @Допустим("я создаю пустое множество атрибутов")
    public void создаю_пустое_множество_атрибутов() {
        attrSet = new TreeSet<>();
    }

    @Допустим("я добавляю атрибут {string} в множество")
    public void добавляю_атрибут(String attr) {
        attrSet.add(attr);
    }

    @Когда("я вызываю операцию computeExtent для этого множества атрибутов")
    public void вычисляю_экстент() {
        resultSet = shared.context.computeExtent(attrSet);
    }

    @Тогда("в результате {int} элементов")
    public void результат_содержит_объектов(int count) {
        assertEquals(count, resultSet.size());
    }

    @Тогда("результат содержит объект {string}")
    public void результат_содержит_объект(String obj) {
        assertTrue(resultSet.contains(obj), "Ожидался объект: " + obj);
    }

    @Тогда("результат не содержит объект {string}")
    public void результат_не_содержит_объект(String obj) {
        assertFalse(resultSet.contains(obj), "Не ожидался объект: " + obj);
    }

    // ── Декларативный стиль ──

    @Когда("я вычисляю общие атрибуты объектов {string} и {string}")
    public void вычисляю_общие_атрибуты(String obj1, String obj2) {
        Set<String> objSet = new TreeSet<>(Set.of(obj1, obj2));
        resultSet = shared.context.computeIntent(objSet);
    }

    @Тогда("общие атрибуты — это {string} и {string}")
    public void общие_атрибуты_это(String attr1, String attr2) {
        Set<String> expected = new TreeSet<>(Set.of(attr1, attr2));
        assertEquals(expected, resultSet);
    }

    // ── Замыкание ──

    @Когда("я вычисляю замыкание для атрибута {string}")
    public void вычисляю_замыкание(String attr) {
        resultSet = shared.context.computeClosure(Set.of(attr));
    }

    @Тогда("замыкание содержит атрибут {string}")
    public void замыкание_содержит_атрибут(String attr) {
        assertTrue(resultSet.contains(attr), "Замыкание не содержит: " + attr);
    }

    // ── Граничные случаи ──

    @Когда("я вычисляю экстент для пустого множества атрибутов")
    public void вычисляю_экстент_пустого_множества() {
        resultSet = shared.context.computeExtent(Collections.emptySet());
    }

    @Когда("я вычисляю интент для пустого множества объектов")
    public void вычисляю_интент_пустого_множества() {
        resultSet = shared.context.computeIntent(Collections.emptySet());
    }

    @Тогда("результат-объекты содержит {int} элементов")
    public void результат_содержит_n_объектов(int count) {
        assertEquals(count, resultSet.size());
    }

    @Тогда("результат-атрибуты содержит {int} элементов")
    public void результат_содержит_n_атрибутов(int count) {
        assertEquals(count, resultSet.size());
    }
}
