package com.fca.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.fca.io.JsonDataLoader;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDataLoaderFuzzTest {

    private final JsonDataLoader loader = new JsonDataLoader();

    @Test
    public void seedExtraColumns() {
        String json = "{\"objects\":[\"o1\"],\"attributes\":[\"a1\"],"
                + "\"incidence\":[[true,false,true]]}";
        assertThrows(JsonParseException.class, () -> loader.parseString(json),
                "Ожидалась JsonParseException при несовпадении размерности incidence");
    }

    @Test
    public void seedExtraRows() {
        String json = "{\"objects\":[\"o1\"],\"attributes\":[\"a1\"],"
                + "\"incidence\":[[true],[false],[true]]}";
        assertThrows(JsonParseException.class, () -> loader.parseString(json),
                "Ожидалась JsonParseException при лишних строках incidence");
    }

    @FuzzTest(maxDuration = "60s")
    public void fuzzParseStructuredJson(FuzzedDataProvider data) {
        int numObjects = data.consumeInt(0, 20);
        int numAttributes = data.consumeInt(0, 20);
        int incidenceRows = data.consumeInt(0, 25);
        int incidenceCols = data.consumeInt(0, 25);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"objects\":[");
        for (int i = 0; i < numObjects; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"obj").append(i).append("\"");
        }
        sb.append("],\"attributes\":[");
        for (int i = 0; i < numAttributes; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"attr").append(i).append("\"");
        }
        sb.append("],\"incidence\":[");
        for (int i = 0; i < incidenceRows; i++) {
            if (i > 0) sb.append(",");
            sb.append("[");
            for (int j = 0; j < incidenceCols; j++) {
                if (j > 0) sb.append(",");
                sb.append(data.consumeBoolean());
            }
            sb.append("]");
        }
        sb.append("]}");

        try {
            loader.parseString(sb.toString());
        } catch (ArrayIndexOutOfBoundsException e) {
            throw e;
        } catch (Exception e) {
            // JsonParseException, IllegalArgumentException — допустимо
        }
    }
}
