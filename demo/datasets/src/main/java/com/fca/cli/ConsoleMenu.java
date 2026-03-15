package com.fca.cli;

import com.fca.algorithm.BitSetCbo;
import com.fca.algorithm.CboAlgorithm;
import com.fca.algorithm.CollectionCbo;
import com.fca.io.DataLoader;
import com.fca.io.ResultWriter;
import com.fca.model.AnalysisResult;
import com.fca.model.FormalContext;
import com.fca.service.ComparisonResult;
import com.fca.service.ComparisonService;

import java.io.PrintStream;
import java.util.*;

/**
 * Консольное текстовое меню приложения.
 */
public class ConsoleMenu {

    private final Scanner scanner;
    private final PrintStream out;
    private final DataLoader dataLoader;
    private final ResultWriter resultWriter;
    private final ComparisonService comparisonService;

    private FormalContext currentContext;
    private AnalysisResult lastResult;

    public ConsoleMenu(Scanner scanner, PrintStream out,
                       DataLoader dataLoader, ResultWriter resultWriter,
                       ComparisonService comparisonService) {
        this.scanner = Objects.requireNonNull(scanner);
        this.out = Objects.requireNonNull(out);
        this.dataLoader = Objects.requireNonNull(dataLoader);
        this.resultWriter = Objects.requireNonNull(resultWriter);
        this.comparisonService = Objects.requireNonNull(comparisonService);
    }

    public void run() {
        out.println("╔══════════════════════════════════════════════════════════╗");
        out.println("║  CbO — Поиск замкнутых импликативных правил             ║");
        out.println("║  Formal Concept Analysis: Close-by-One Algorithm        ║");
        out.println("╚══════════════════════════════════════════════════════════╝");
        out.println();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> showHelp();
                case "2" -> enterContextManually();
                case "3" -> loadContextFromJson();
                case "4" -> runCbo(new CollectionCbo());
                case "5" -> runCbo(new BitSetCbo());
                case "6" -> compareBoth();
                case "7" -> saveResult();
                case "8" -> showContext();
                case "0" -> {
                    out.println("Выход. До свидания!");
                    running = false;
                }
                default -> out.println("Неверный выбор. Повторите ввод.");
            }
            out.println();
        }
    }

    private void printMenu() {
        out.println("────────────── МЕНЮ ──────────────");
        out.println("  1. Справка");
        out.println("  2. Ввод контекста вручную");
        out.println("  3. Загрузка контекста из JSON");
        out.println("  4. Запуск CbO (Collections)");
        out.println("  5. Запуск CbO (BitSet)");
        out.println("  6. Сравнение двух реализаций");
        out.println("  7. Сохранить результат в JSON");
        out.println("  8. Показать текущий контекст");
        out.println("  0. Выход");
        out.print("Ваш выбор: ");
    }

    private void showHelp() {
        out.println();
        out.println("=== СПРАВКА ===");
        out.println("Данное приложение реализует алгоритм Close-by-One (CbO)");
        out.println("для поиска всех формальных понятий и замкнутых импликативных правил");
        out.println("в заданном формальном контексте (Formal Concept Analysis).");
        out.println();
        out.println("Формальный контекст — тройка (G, M, I), где:");
        out.println("  G — множество объектов");
        out.println("  M — множество атрибутов");
        out.println("  I ⊆ G × M — бинарное отношение инцидентности");
        out.println();
        out.println("Формальное понятие — пара (A, B), где A ⊆ G, B ⊆ M,");
        out.println("  A' = B (атрибуты общие для всех объектов из A),");
        out.println("  B' = A (объекты, обладающие всеми атрибутами из B).");
        out.println();
        out.println("Импликативное правило A → B означает: если объект имеет");
        out.println("все атрибуты из A, то он обязательно имеет и атрибуты из B.");
        out.println();
        out.println("Алгоритм CbO перечисляет все понятия, начиная с верхнего");
        out.println("(все объекты), рекурсивно добавляя по одному атрибуту");
        out.println("с проверкой каноничности для избежания дублирования.");
        out.println();
        out.println("Две реализации:");
        out.println("  • Collections — на основе HashSet/ArrayList");
        out.println("  • BitSet — на основе побитовых операций (быстрее на больших данных)");
        out.println();
        out.println("⚠ Число формальных понятий может расти экспоненциально");
        out.println("  (до 2^min(|G|,|M|)). Для больших контекстов используйте осторожно.");
    }

    private void enterContextManually() {
        try {
            out.print("Введите имена объектов через запятую: ");
            String objLine = scanner.nextLine().trim();
            if (objLine.isEmpty()) {
                out.println("Ошибка: пустой ввод объектов.");
                return;
            }
            List<String> objects = Arrays.stream(objLine.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();

            out.print("Введите имена атрибутов через запятую: ");
            String attrLine = scanner.nextLine().trim();
            if (attrLine.isEmpty()) {
                out.println("Ошибка: пустой ввод атрибутов.");
                return;
            }
            List<String> attributes = Arrays.stream(attrLine.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();

            boolean[][] incidence = new boolean[objects.size()][attributes.size()];
            out.println("Введите матрицу инцидентности (1/0 или +/- через пробел):");
            for (int i = 0; i < objects.size(); i++) {
                out.print("  " + objects.get(i) + ": ");
                String line = scanner.nextLine().trim();
                String[] tokens = line.split("\\s+");
                if (tokens.length != attributes.size()) {
                    out.println("Ошибка: ожидалось " + attributes.size() + " значений, получено " + tokens.length);
                    return;
                }
                for (int j = 0; j < attributes.size(); j++) {
                    incidence[i][j] = tokens[j].equals("1") || tokens[j].equals("+");
                }
            }

            currentContext = new FormalContext(objects, attributes, incidence);
            lastResult = null;
            out.println("Контекст успешно создан.");
            out.println(currentContext);
        } catch (Exception e) {
            out.println("Ошибка при вводе: " + e.getMessage());
        }
    }

    private void loadContextFromJson() {
        out.print("Введите путь к JSON-файлу (или Enter для загрузки примера): ");
        String path = scanner.nextLine().trim();
        try {
            if (path.isEmpty()) {
                currentContext = loadSampleContext();
                out.println("Загружен пример контекста из ресурсов.");
            } else {
                currentContext = dataLoader.load(path);
                out.println("Контекст успешно загружен.");
            }
            lastResult = null;
            out.println(currentContext);
        } catch (Exception e) {
            out.println("Ошибка при загрузке: " + e.getMessage());
        }
    }

    private FormalContext loadSampleContext() throws Exception {
        try (var is = getClass().getClassLoader().getResourceAsStream("sample-context.json")) {
            if (is == null) {
                throw new java.io.IOException("Файл sample-context.json не найден в ресурсах");
            }
            var reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
            return ((com.fca.io.JsonDataLoader) dataLoader).parse(reader);
        }
    }

    private void runCbo(CboAlgorithm algorithm) {
        if (currentContext == null) {
            out.println("Сначала введите или загрузите формальный контекст.");
            return;
        }
        try {
            out.println("Запуск " + algorithm.getName() + "...");
            lastResult = comparisonService.runAlgorithm(currentContext, algorithm);
            out.println(lastResult);
        } catch (Exception e) {
            out.println("Ошибка при выполнении: " + e.getMessage());
        }
    }

    private void compareBoth() {
        if (currentContext == null) {
            out.println("Сначала введите или загрузите формальный контекст.");
            return;
        }
        try {
            out.println("Сравнение двух реализаций...");
            ComparisonResult result = comparisonService.compare(
                    currentContext, new CollectionCbo(), new BitSetCbo());
            lastResult = result.getResult1();
            out.println(result.getSummary());
            if (result.isFullyConsistent()) {
                out.println("✓ Обе реализации дали идентичные результаты.");
            } else {
                out.println("✗ ВНИМАНИЕ: результаты реализаций различаются!");
            }
        } catch (Exception e) {
            out.println("Ошибка при сравнении: " + e.getMessage());
        }
    }

    private static final String DEFAULT_RESULT_PATH = "result.json";

    private void saveResult() {
        if (lastResult == null) {
            out.println("Нет результатов для сохранения. Сначала запустите алгоритм.");
            return;
        }
        out.print("Введите путь для сохранения JSON (или Enter для '" + DEFAULT_RESULT_PATH + "'): ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) {
            path = DEFAULT_RESULT_PATH;
        }
        try {
            resultWriter.write(lastResult, path);
            out.println("Результат сохранён в: " + path);
        } catch (Exception e) {
            out.println("Ошибка при сохранении: " + e.getMessage());
        }
    }

    private void showContext() {
        if (currentContext == null) {
            out.println("Контекст не загружен.");
        } else {
            out.println(currentContext);
        }
    }
}
