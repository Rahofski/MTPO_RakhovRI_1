package com.fca.bdd;

import com.fca.io.JsonDataLoader;
import com.fca.model.FormalContext;
import io.cucumber.java.ru.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class DataLoadingSteps {

    private final JsonDataLoader loader = new JsonDataLoader();
    private String jsonString;
    private FormalContext context;
    private Exception caughtException;

    @Допустим("существует загрузчик JSON-данных")
    public void существует_загрузчик() {
        assertNotNull(loader);
    }

    @Допустим("задан JSON-контекст:")
    public void задан_json_контекст(String json) {
        this.jsonString = json;
    }

    @Когда("я загружаю контекст из этой JSON-строки")
    public void загружаю_контекст_из_json_строки() {
        context = loader.parseString(jsonString);
    }

    @Когда("я пытаюсь загрузить контекст из этой JSON-строки")
    public void пытаюсь_загрузить_из_json_строки() {
        try {
            context = loader.parseString(jsonString);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Когда("я загружаю контекст из файла {string}")
    public void загружаю_контекст_из_файла(String filename) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        assertNotNull(is, "Ресурс не найден: " + filename);
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            context = loader.parse(reader);
        }
    }

    @Когда("я пытаюсь загрузить контекст из файла {string}")
    public void пытаюсь_загрузить_из_файла(String filename) {
        try {
            context = loader.load(filename);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Тогда("контекст содержит {int} объектов")
    public void контекст_содержит_объектов(int count) {
        assertEquals(count, context.getObjectCount());
    }

    @Тогда("контекст содержит {int} атрибутов")
    public void контекст_содержит_атрибутов(int count) {
        assertEquals(count, context.getAttributeCount());
    }

    @Тогда("объект {string} обладает атрибутом {string}")
    public void объект_обладает_атрибутом(String obj, String attr) {
        int i = context.getObjects().indexOf(obj);
        int j = context.getAttributes().indexOf(attr);
        assertTrue(i >= 0, "Объект не найден: " + obj);
        assertTrue(j >= 0, "Атрибут не найден: " + attr);
        assertTrue(context.hasRelation(i, j));
    }

    @Тогда("объект {string} не обладает атрибутом {string}")
    public void объект_не_обладает_атрибутом(String obj, String attr) {
        int i = context.getObjects().indexOf(obj);
        int j = context.getAttributes().indexOf(attr);
        assertTrue(i >= 0 && j >= 0);
        assertFalse(context.hasRelation(i, j));
    }

    @Тогда("возникает ошибка парсинга с сообщением {string}")
    public void возникает_ошибка_парсинга(String msg) {
        assertNotNull(caughtException, "Ожидалось исключение");
        assertTrue(caughtException.getMessage().contains(msg),
                "Сообщение '" + caughtException.getMessage() + "' не содержит '" + msg + "'");
    }

    @Тогда("возникает ошибка ввода-вывода")
    public void возникает_ошибка_io() {
        assertNotNull(caughtException, "Ожидалось исключение");
        assertTrue(caughtException instanceof IOException,
                "Ожидалась IOException, получена: " + caughtException.getClass());
    }
}
