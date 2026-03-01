package com.fca.io;

import com.fca.model.FormalContext;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonDataLoader.
 *
 * Техники:
 * - Boundary Value Analysis: пустой JSON, минимальный контекст
 * - Equivalence Partitioning: корректный JSON, некорректный JSON, отсутствующий файл
 * - Branch Testing: покрытие всех ветвей парсинга
 */
class JsonDataLoaderTest {

    private final JsonDataLoader loader = new JsonDataLoader();

    // ===== Позитивные тесты =====
    @Test
    @DisplayName("Парсинг корректного JSON")
    void parseValidJson() {
        String json = """
                {
                  "objects": ["o1", "o2"],
                  "attributes": ["a1", "a2"],
                  "incidence": [[true, false], [false, true]]
                }
                """;
        FormalContext ctx = loader.parseString(json);
        assertAll(
                () -> assertEquals(2, ctx.getObjectCount()),
                () -> assertEquals(2, ctx.getAttributeCount()),
                () -> assertTrue(ctx.hasRelation(0, 0)),
                () -> assertFalse(ctx.hasRelation(0, 1)),
                () -> assertFalse(ctx.hasRelation(1, 0)),
                () -> assertTrue(ctx.hasRelation(1, 1))
        );
    }

    @Test
    @DisplayName("Парсинг пустого контекста")
    void parseEmptyContext() {
        String json = """
                {
                  "objects": [],
                  "attributes": [],
                  "incidence": []
                }
                """;
        FormalContext ctx = loader.parseString(json);
        assertEquals(0, ctx.getObjectCount());
        assertEquals(0, ctx.getAttributeCount());
    }

    @Test
    @DisplayName("Парсинг из Reader")
    void parseFromReader() {
        String json = """
                {
                  "objects": ["x"],
                  "attributes": ["a"],
                  "incidence": [[true]]
                }
                """;
        FormalContext ctx = loader.parse(new StringReader(json));
        assertTrue(ctx.hasRelation(0, 0));
    }

    @Test
    @DisplayName("Загрузка из файла")
    void loadFromFile() throws Exception {
        Path tmpFile = Files.createTempFile("fca-test-", ".json");
        try {
            String json = """
                    {
                      "objects": ["a"],
                      "attributes": ["x"],
                      "incidence": [[true]]
                    }
                    """;
            Files.writeString(tmpFile, json);
            FormalContext ctx = loader.load(tmpFile.toString());
            assertEquals(1, ctx.getObjectCount());
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    // ===== Негативные тесты =====
    @Test
    @DisplayName("Negative: файл не существует → IOException")
    void fileNotFound() {
        assertThrows(IOException.class, () -> loader.load("nonexistent.json"));
    }

    @Test
    @DisplayName("Negative: отсутствует поле objects")
    void missingObjects() {
        String json = """
                { "attributes": ["a"], "incidence": [[true]] }
                """;
        assertThrows(JsonParseException.class, () -> loader.parseString(json));
    }

    @Test
    @DisplayName("Negative: отсутствует поле attributes")
    void missingAttributes() {
        String json = """
                { "objects": ["x"], "incidence": [[true]] }
                """;
        assertThrows(JsonParseException.class, () -> loader.parseString(json));
    }

    @Test
    @DisplayName("Negative: отсутствует поле incidence")
    void missingIncidence() {
        String json = """
                { "objects": ["x"], "attributes": ["a"] }
                """;
        assertThrows(JsonParseException.class, () -> loader.parseString(json));
    }

    @Test
    @DisplayName("Negative: несовпадение строк incidence")
    void mismatchedIncidenceRows() {
        String json = """
                {
                  "objects": ["o1", "o2"],
                  "attributes": ["a"],
                  "incidence": [[true]]
                }
                """;
        assertThrows(JsonParseException.class, () -> loader.parseString(json));
    }

    @Test
    @DisplayName("Negative: несовпадение столбцов incidence")
    void mismatchedIncidenceCols() {
        String json = """
                {
                  "objects": ["o1"],
                  "attributes": ["a", "b"],
                  "incidence": [[true]]
                }
                """;
        assertThrows(JsonParseException.class, () -> loader.parseString(json));
    }

    @Test
    @DisplayName("Negative: невалидный JSON")
    void invalidJson() {
        assertThrows(Exception.class, () -> loader.parseString("not a json"));
    }

    // ===== Параметризованный тест: невалидные JSON =====
    @ParameterizedTest(name = "Невалидный JSON: {0}")
    @ValueSource(strings = {
            "{}",
            "{\"objects\": []}",
            "{\"objects\": [], \"attributes\": []}"
    })
    @DisplayName("Negative: неполный JSON вызывает исключение")
    void incompleteJson(String json) {
        assertThrows(JsonParseException.class, () -> loader.parseString(json));
    }
}
