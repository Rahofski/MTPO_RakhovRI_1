package com.fca.io;

import com.fca.model.*;
import com.google.gson.*;
import org.junit.jupiter.api.*;

import java.nio.file.*;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonResultWriter.
 *
 * Техники:
 * - Boundary Value Analysis: пустой результат
 * - Equivalence Partitioning: результат с понятиями, без импликаций
 * - Branch Testing: запись в файл, генерация JSON
 */
class JsonResultWriterTest {

    private final JsonResultWriter writer = new JsonResultWriter();

    private AnalysisResult sampleResult() {
        List<FormalConcept> concepts = List.of(
                new FormalConcept(Set.of("o1", "o2"), Set.of("a")),
                new FormalConcept(Set.of("o1"), Set.of("a", "b"))
        );
        List<Implication> implications = List.of(
                new Implication(Set.of("a", "c"), Set.of("d"))
        );
        return new AnalysisResult(concepts, implications, 3, 4, 1_500_000, "TestAlgo");
    }

    @Test
    @DisplayName("toJson содержит все обязательные поля")
    void toJsonContainsAllFields() {
        String json = writer.toJson(sampleResult());
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        assertAll(
                () -> assertTrue(root.has("algorithmName")),
                () -> assertTrue(root.has("statistics")),
                () -> assertTrue(root.has("concepts")),
                () -> assertTrue(root.has("implications"))
        );
    }

    @Test
    @DisplayName("Статистика содержит правильные значения")
    void statisticsValues() {
        String json = writer.toJson(sampleResult());
        JsonObject stats = JsonParser.parseString(json).getAsJsonObject()
                .getAsJsonObject("statistics");

        assertAll(
                () -> assertEquals(3, stats.get("objectCount").getAsInt()),
                () -> assertEquals(4, stats.get("attributeCount").getAsInt()),
                () -> assertEquals(2, stats.get("conceptCount").getAsInt()),
                () -> assertEquals(1, stats.get("implicationCount").getAsInt())
        );
    }

    @Test
    @DisplayName("Понятия корректно сериализуются")
    void conceptsSerialized() {
        String json = writer.toJson(sampleResult());
        JsonArray concepts = JsonParser.parseString(json).getAsJsonObject()
                .getAsJsonArray("concepts");
        assertEquals(2, concepts.size());

        JsonObject first = concepts.get(0).getAsJsonObject();
        assertTrue(first.has("extent"));
        assertTrue(first.has("intent"));
    }

    @Test
    @DisplayName("Импликации корректно сериализуются")
    void implicationsSerialized() {
        String json = writer.toJson(sampleResult());
        JsonArray impls = JsonParser.parseString(json).getAsJsonObject()
                .getAsJsonArray("implications");
        assertEquals(1, impls.size());

        JsonObject impl = impls.get(0).getAsJsonObject();
        assertTrue(impl.has("premise"));
        assertTrue(impl.has("conclusion"));
    }

    @Test
    @DisplayName("BVA: пустой результат")
    void emptyResult() {
        AnalysisResult empty = new AnalysisResult(
                List.of(), List.of(), 0, 0, 0, "empty");
        String json = writer.toJson(empty);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(0, root.getAsJsonArray("concepts").size());
        assertEquals(0, root.getAsJsonArray("implications").size());
    }

    @Test
    @DisplayName("Запись в файл и чтение обратно")
    void writeAndRead() throws Exception {
        Path tmpFile = Files.createTempFile("fca-result-", ".json");
        try {
            writer.write(sampleResult(), tmpFile.toString());
            assertTrue(Files.exists(tmpFile));
            String content = Files.readString(tmpFile);
            assertThat(content, containsString("algorithmName"));
            assertThat(content, containsString("TestAlgo"));
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    @Test
    @DisplayName("algorithmName корректно записывается")
    void algorithmNameWritten() {
        String json = writer.toJson(sampleResult());
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("TestAlgo", root.get("algorithmName").getAsString());
    }
}
