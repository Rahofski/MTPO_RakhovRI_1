package com.fca.io;

import com.fca.model.AnalysisResult;
import com.fca.model.FormalConcept;
import com.fca.model.Implication;
import com.google.gson.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Запись результатов анализа в JSON-файл.
 * <p>
 * Формат выходного JSON:
 * <pre>
 * {
 *   "algorithmName": "...",
 *   "statistics": { ... },
 *   "concepts": [ { "extent": [...], "intent": [...] }, ... ],
 *   "implications": [ { "premise": [...], "conclusion": [...] }, ... ]
 * }
 * </pre>
 */
public class JsonResultWriter implements ResultWriter {

    @Override
    public void write(AnalysisResult result, String destination) throws IOException {
        Path path = Path.of(destination);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(toJson(result));
        }
    }

    public String toJson(AnalysisResult result) {
        JsonObject root = new JsonObject();
        root.addProperty("algorithmName", result.getAlgorithmName());

        // Статистика
        JsonObject stats = new JsonObject();
        stats.addProperty("objectCount", result.getObjectCount());
        stats.addProperty("attributeCount", result.getAttributeCount());
        stats.addProperty("conceptCount", result.getConceptCount());
        stats.addProperty("implicationCount", result.getImplicationCount());
        stats.addProperty("executionTimeMs", Math.round(result.getExecutionTimeMs() * 1000.0) / 1000.0);
        root.add("statistics", stats);

        // Понятия
        JsonArray conceptsArr = new JsonArray();
        for (FormalConcept c : result.getConcepts()) {
            JsonObject cObj = new JsonObject();
            cObj.add("extent", toJsonArray(c.getExtent()));
            cObj.add("intent", toJsonArray(c.getIntent()));
            conceptsArr.add(cObj);
        }
        root.add("concepts", conceptsArr);

        // Импликации
        JsonArray implArr = new JsonArray();
        for (Implication imp : result.getImplications()) {
            JsonObject iObj = new JsonObject();
            iObj.add("premise", toJsonArray(imp.getPremise()));
            iObj.add("conclusion", toJsonArray(imp.getConclusion()));
            implArr.add(iObj);
        }
        root.add("implications", implArr);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(root);
    }

    private JsonArray toJsonArray(Iterable<String> items) {
        JsonArray arr = new JsonArray();
        for (String item : items) {
            arr.add(item);
        }
        return arr;
    }
}
