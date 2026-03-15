package com.fca.io;

import com.fca.model.FormalContext;
import com.google.gson.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Загрузчик формального контекста из JSON-файла.
 * <p>
 * Формат входного JSON:
 * <pre>
 * {
 *   "objects": ["obj1", "obj2", ...],
 *   "attributes": ["attr1", "attr2", ...],
 *   "incidence": [[true, false, ...], [false, true, ...], ...]
 * }
 * </pre>
 */
public class JsonDataLoader implements DataLoader {

    @Override
    public FormalContext load(String source) throws IOException {
        Path path = Path.of(source);
        if (!Files.exists(path)) {
            throw new IOException("Файл не найден: " + source);
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }

    /**
     * Парсинг формального контекста из Reader (удобно для тестирования).
     */
    public FormalContext parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        return parseJsonObject(root);
    }

    /**
     * Парсинг формального контекста из строки JSON.
     */
    public FormalContext parseString(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return parseJsonObject(root);
    }

    private FormalContext parseJsonObject(JsonObject root) {
        if (!root.has("objects") || !root.has("attributes") || !root.has("incidence")) {
            throw new JsonParseException("JSON должен содержать поля: objects, attributes, incidence");
        }

        JsonArray objArr = root.getAsJsonArray("objects");
        JsonArray attrArr = root.getAsJsonArray("attributes");
        JsonArray incArr = root.getAsJsonArray("incidence");

        List<String> objects = new ArrayList<>();
        for (JsonElement e : objArr) {
            objects.add(e.getAsString());
        }

        List<String> attributes = new ArrayList<>();
        for (JsonElement e : attrArr) {
            attributes.add(e.getAsString());
        }

        if (incArr.size() != objects.size()) {
            throw new JsonParseException(
                    "Число строк incidence (" + incArr.size() +
                            ") не совпадает с числом объектов (" + objects.size() + ")");
        }

        boolean[][] incidence = new boolean[objects.size()][attributes.size()];
        for (int i = 0; i < incArr.size(); i++) {
            JsonArray row = incArr.get(i).getAsJsonArray();
            if (row.size() != attributes.size()) {
                throw new JsonParseException(
                        "Строка " + i + " incidence имеет " + row.size() +
                                " элементов, ожидалось " + attributes.size());
            }
            for (int j = 0; j < row.size(); j++) {
                incidence[i][j] = row.get(j).getAsBoolean();
            }
        }

        return new FormalContext(objects, attributes, incidence);
    }
}
