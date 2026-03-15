package com.fca.dataset;

import com.fca.model.FormalContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class DatasetGenerator {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FormalContext generate(int nObj, int nAttr, double density, long seed) {
        Random rnd = new Random(seed);
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

    public void saveToJson(FormalContext context, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());

        JsonObject root = new JsonObject();

        JsonArray objArr = new JsonArray();
        for (String obj : context.getObjects()) objArr.add(obj);
        root.add("objects", objArr);

        JsonArray attrArr = new JsonArray();
        for (String attr : context.getAttributes()) attrArr.add(attr);
        root.add("attributes", attrArr);

        JsonArray incArr = new JsonArray();
        for (int i = 0; i < context.getObjectCount(); i++) {
            JsonArray row = new JsonArray();
            for (int j = 0; j < context.getAttributeCount(); j++) {
                row.add(context.hasRelation(i, j));
            }
            incArr.add(row);
        }
        root.add("incidence", incArr);

        JsonObject meta = new JsonObject();
        meta.addProperty("objectCount", context.getObjectCount());
        meta.addProperty("attributeCount", context.getAttributeCount());
        meta.addProperty("density", computeDensity(context));
        meta.addProperty("seed", 42);
        root.add("_meta", meta);

        Files.writeString(filePath, gson.toJson(root), StandardCharsets.UTF_8);
    }

    public static String buildFilename(String typeCode, String sizeCode,
                                        int nObj, int nAttr, double density) {
        int densityPct = (int) Math.round(density * 100);
        return String.format("%s-%s_%dx%d_d%03d.json", typeCode, sizeCode, nObj, nAttr, densityPct);
    }

    public static double computeDensity(FormalContext ctx) {
        int total = ctx.getObjectCount() * ctx.getAttributeCount();
        if (total == 0) return 0;
        int trueCount = 0;
        for (int i = 0; i < ctx.getObjectCount(); i++)
            for (int j = 0; j < ctx.getAttributeCount(); j++)
                if (ctx.hasRelation(i, j)) trueCount++;
        return (double) trueCount / total;
    }
}
