package com.fca.experiment;

import com.fca.model.FormalContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Генерация максимальных датасетов, найденных предварительным экспериментом.
 *
 * Запуск:
 *   mvn compile -q
 *   java -cp "target/classes;target/dependency/*" com.fca.experiment.GenerateMaxDatasets
 */
public class GenerateMaxDatasets {

    static final long SEED = 42;

    public static void main(String[] args) throws IOException {
        Path datasetsDir = Path.of("datasets");
        Path resourcesDir = Path.of("src/test/resources/datasets");

        // DN-MAX: 34x31, d=0.70 — обработка ~2.5 минуты (150.8 с)
        generate("DN-MAX_34x31_d070.json", 34, 31, 0.70, datasetsDir, resourcesDir);

        // SP-MAX: 99x95, d=0.20 — обработка ~64 с (максимальный замеренный)
        generate("SP-MAX_99x95_d020.json", 99, 95, 0.20, datasetsDir, resourcesDir);

        System.out.println("Done. Generated 2 max datasets.");
    }

    static void generate(String filename, int nObj, int nAttr, double density,
                         Path... dirs) throws IOException {
        FormalContext ctx = makeContext(nObj, nAttr, density);
        String json = toJson(ctx, density);

        for (Path dir : dirs) {
            Files.createDirectories(dir);
            Path file = dir.resolve(filename);
            Files.writeString(file, json, StandardCharsets.UTF_8);
            System.out.printf("  Written: %s (%d bytes)%n", file, json.length());
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

    static String toJson(FormalContext ctx, double density) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();

        JsonArray objArr = new JsonArray();
        for (String obj : ctx.getObjects()) objArr.add(obj);
        root.add("objects", objArr);

        JsonArray attrArr = new JsonArray();
        for (String attr : ctx.getAttributes()) attrArr.add(attr);
        root.add("attributes", attrArr);

        JsonArray incArr = new JsonArray();
        for (int i = 0; i < ctx.getObjectCount(); i++) {
            JsonArray row = new JsonArray();
            for (int j = 0; j < ctx.getAttributeCount(); j++) {
                row.add(ctx.hasRelation(i, j));
            }
            incArr.add(row);
        }
        root.add("incidence", incArr);

        JsonObject meta = new JsonObject();
        meta.addProperty("objectCount", ctx.getObjectCount());
        meta.addProperty("attributeCount", ctx.getAttributeCount());
        meta.addProperty("density", density);
        meta.addProperty("seed", SEED);
        root.add("_meta", meta);

        return gson.toJson(root);
    }
}
