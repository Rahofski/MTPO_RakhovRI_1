package com.fca.bdd;

import com.fca.algorithm.CboAlgorithm;
import com.fca.implication.ImplicationGenerator;
import com.fca.io.JsonResultWriter;
import com.fca.model.AnalysisResult;
import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import com.fca.model.Implication;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cucumber.java.ru.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ResultExportSteps {

    private AnalysisResult analysisResult;
    private String jsonOutput;
    private Path outputFilePath;

    @Допустим("выполнен анализ контекста с {int} объектами и {int} атрибутами алгоритмом {string}")
    public void выполнен_анализ(int nObj, int nAttr, String algoName) {
        // Используем sample-context.json (5x6) или генерируем
        List<String> objects = new ArrayList<>();
        for (int i = 0; i < nObj; i++) objects.add("obj" + i);
        List<String> attributes = new ArrayList<>();
        for (int j = 0; j < nAttr; j++) attributes.add("attr" + j);

        boolean[][] incidence = new boolean[nObj][nAttr];
        Random rnd = new Random(42);
        for (int i = 0; i < nObj; i++)
            for (int j = 0; j < nAttr; j++)
                incidence[i][j] = rnd.nextDouble() < 0.5;

        FormalContext context = new FormalContext(objects, attributes, incidence);
        CboAlgorithm algo = ConceptComputationSteps.createAlgorithm(algoName);

        long start = System.nanoTime();
        List<FormalConcept> concepts = algo.computeConcepts(context);
        long elapsed = System.nanoTime() - start;

        List<Implication> implications = new ImplicationGenerator().generate(context, concepts);
        analysisResult = new AnalysisResult(concepts, implications, nObj, nAttr, elapsed, algoName);
    }

    @Допустим("выполнен анализ пустого контекста")
    public void выполнен_анализ_пустого() {
        analysisResult = new AnalysisResult(
                Collections.emptyList(), Collections.emptyList(),
                0, 0, 0L, "empty"
        );
    }

    @Когда("я сериализую результат в JSON")
    public void сериализую_в_json() {
        JsonResultWriter writer = new JsonResultWriter();
        jsonOutput = writer.toJson(analysisResult);
    }

    @Когда("я записываю результат в файл {string}")
    public void записываю_в_файл(String filename) throws IOException {
        JsonResultWriter writer = new JsonResultWriter();
        outputFilePath = Path.of(System.getProperty("java.io.tmpdir"), filename);
        writer.write(analysisResult, outputFilePath.toString());
    }

    @Тогда("JSON содержит поле {string} со значением {string}")
    public void json_содержит_поле(String field, String value) {
        JsonObject root = JsonParser.parseString(jsonOutput).getAsJsonObject();
        assertTrue(root.has(field), "JSON не содержит поле: " + field);
        assertEquals(value, root.get(field).getAsString());
    }

    @Тогда("JSON содержит секцию {string}")
    public void json_содержит_секцию(String section) {
        JsonObject root = JsonParser.parseString(jsonOutput).getAsJsonObject();
        assertTrue(root.has(section), "JSON не содержит секцию: " + section);
    }

    @Тогда("в секции statistics поле {string} равно {int}")
    public void статистика_поле_равно(String field, int value) {
        JsonObject root = JsonParser.parseString(jsonOutput).getAsJsonObject();
        JsonObject stats = root.getAsJsonObject("statistics");
        assertEquals(value, stats.get(field).getAsInt());
    }

    @Тогда("файл {string} существует")
    public void файл_существует(String filename) {
        assertTrue(Files.exists(outputFilePath), "Файл не найден: " + outputFilePath);
    }

    @Тогда("содержимое файла является валидным JSON")
    public void файл_валидный_json() throws IOException {
        String content = Files.readString(outputFilePath);
        assertDoesNotThrow(() -> JsonParser.parseString(content),
                "Содержимое файла — невалидный JSON");
        // Удаляем временный файл
        Files.deleteIfExists(outputFilePath);
    }
}
