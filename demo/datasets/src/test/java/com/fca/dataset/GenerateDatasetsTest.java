package com.fca.dataset;

import com.fca.model.FormalContext;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Генерация и сохранение всех тестовых датасетов для нагрузочного тестирования.
 * <p>
 * Кодировка названий наборов данных:
 * <pre>
 * ┌──────────┬────────────────────────────────────────────────────────────────┐
 * │  Код     │  Расшифровка                                                 │
 * ├──────────┼────────────────────────────────────────────────────────────────┤
 * │  SP      │  Sparse — разреженная матрица (density ≈ 20%)                │
 * │  DN      │  Dense  — плотная матрица    (density ≈ 70%)                 │
 * │  S       │  Small  — малый размер                                      │
 * │  L       │  Large  — большой / «максимальный» размер                   │
 * ├──────────┼────────────────────────────────────────────────────────────────┤
 * │  SP-S    │  Разреженный малый:    10 объектов × 8 атрибутов,  d=0.20   │
 * │  SP-L    │  Разреженный большой:  18 объектов × 14 атрибутов, d=0.20   │
 * │  DN-S    │  Плотный малый:         8 объектов × 6 атрибутов,  d=0.70   │
 * │  DN-L    │  Плотный большой:      14 объектов × 12 атрибутов, d=0.70   │
 * ├──────────┼────────────────────────────────────────────────────────────────┤
 * │  DN-8    │  Плотный предв. эксп.:  8 × 6,  d=0.70                     │
 * │  DN-10   │  Плотный предв. эксп.: 10 × 8,  d=0.70                     │
 * │  DN-12   │  Плотный предв. эксп.: 12 × 10, d=0.70                     │
 * │  DN-14   │  Плотный предв. эксп.: 14 × 12, d=0.70                     │
 * │  DN-16   │  Плотный предв. эксп.: 16 × 13, d=0.70                     │
 * └──────────┴────────────────────────────────────────────────────────────────┘
 * </pre>
 * Формат имени файла: {@code <тип>-<размер>_<|G|>x<|M|>_d<DDD>.json},
 * где DDD = плотность × 1000 (три цифры, например d020 = 0.20, d070 = 0.70).
 * <p>
 * Датасеты сохраняются в папку {@code datasets/} в корне проекта и в
 * {@code src/test/resources/datasets/} для загрузки из тестов.
 * <p>
 * Все датасеты генерируются с {@code seed=42} для воспроизводимости.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Генерация датасетов для нагрузочного тестирования")
class GenerateDatasetsTest {

    private static final long SEED = 42L;
    private static final Path DATASETS_DIR = Path.of("datasets");
    private static final Path RESOURCES_DATASETS_DIR =
            Path.of("src/test/resources/datasets");

    private final DatasetGenerator generator = new DatasetGenerator();

    // ═══════════════════════════════════════════════════════════════
    // 4 основных датасета (2 типа × 2 размера)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("SP-S: разреженный малый (10×8, d=0.20)")
    void generateSparseSmall() throws IOException {
        generateAndSave("SP", "S", 10, 8, 0.20);
    }

    @Test
    @Order(2)
    @DisplayName("SP-L: разреженный большой (18×14, d=0.20)")
    void generateSparseLarge() throws IOException {
        generateAndSave("SP", "L", 18, 14, 0.20);
    }

    @Test
    @Order(3)
    @DisplayName("DN-S: плотный малый (8×6, d=0.70)")
    void generateDenseSmall() throws IOException {
        generateAndSave("DN", "S", 8, 6, 0.70);
    }

    @Test
    @Order(4)
    @DisplayName("DN-L: плотный большой / «максимальный» (14×12, d=0.70)")
    void generateDenseLarge() throws IOException {
        generateAndSave("DN", "L", 14, 12, 0.70);
    }

    // ═══════════════════════════════════════════════════════════════
    // Датасеты предварительного эксперимента
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("Предварительный эксперимент: DN-8 (8×6, d=0.70)")
    void generatePrelimDN8() throws IOException {
        generateAndSave("DN", "8", 8, 6, 0.70);
    }

    @Test
    @Order(11)
    @DisplayName("Предварительный эксперимент: DN-10 (10×8, d=0.70)")
    void generatePrelimDN10() throws IOException {
        generateAndSave("DN", "10", 10, 8, 0.70);
    }

    @Test
    @Order(12)
    @DisplayName("Предварительный эксперимент: DN-12 (12×10, d=0.70)")
    void generatePrelimDN12() throws IOException {
        generateAndSave("DN", "12", 12, 10, 0.70);
    }

    @Test
    @Order(13)
    @DisplayName("Предварительный эксперимент: DN-14 (14×12, d=0.70)")
    void generatePrelimDN14() throws IOException {
        generateAndSave("DN", "14", 14, 12, 0.70);
    }

    @Test
    @Order(14)
    @DisplayName("Предварительный эксперимент: DN-16 (16×13, d=0.70)")
    void generatePrelimDN16() throws IOException {
        generateAndSave("DN", "16", 16, 13, 0.70);
    }

    // ═══════════════════════════════════════════════════════════════
    // Датасеты для сводного профилирования (средняя плотность)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @DisplayName("Сводный профиль: MD-6 (6×5, d=0.50)")
    void generateMD6() throws IOException {
        generateAndSave("MD", "6", 6, 5, 0.50);
    }

    @Test
    @Order(21)
    @DisplayName("Сводный профиль: MD-10 (10×8, d=0.50)")
    void generateMD10() throws IOException {
        generateAndSave("MD", "10", 10, 8, 0.50);
    }

    @Test
    @Order(22)
    @DisplayName("Сводный профиль: MD-14 (14×10, d=0.50)")
    void generateMD14() throws IOException {
        generateAndSave("MD", "14", 14, 10, 0.50);
    }

    @Test
    @Order(23)
    @DisplayName("Сводный профиль: MD-16 (16×12, d=0.50)")
    void generateMD16() throws IOException {
        generateAndSave("MD", "16", 16, 12, 0.50);
    }

    // ═══════════════════════════════════════════════════════════════
    // Вспомогательные методы
    // ═══════════════════════════════════════════════════════════════

    private void generateAndSave(String typeCode, String sizeCode,
                                  int nObj, int nAttr, double density) throws IOException {
        FormalContext ctx = generator.generate(nObj, nAttr, density, SEED);
        String filename = DatasetGenerator.buildFilename(typeCode, sizeCode, nObj, nAttr, density);

        // Сохраняем в datasets/ (корень проекта)
        Path mainPath = DATASETS_DIR.resolve(filename);
        generator.saveToJson(ctx, mainPath);
        assertTrue(Files.exists(mainPath), "Файл не создан: " + mainPath);

        // Сохраняем в src/test/resources/datasets/ (для classpath)
        Path resourcePath = RESOURCES_DATASETS_DIR.resolve(filename);
        generator.saveToJson(ctx, resourcePath);
        assertTrue(Files.exists(resourcePath), "Файл не создан: " + resourcePath);

        double actualDensity = DatasetGenerator.computeDensity(ctx);

        System.out.printf("[DATASET] %-30s  %d×%d  target_d=%.2f  actual_d=%.4f  → %s%n",
                filename, nObj, nAttr, density, actualDensity, mainPath.toAbsolutePath());
    }
}
