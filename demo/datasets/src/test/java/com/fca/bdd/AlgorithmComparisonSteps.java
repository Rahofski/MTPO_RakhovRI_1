package com.fca.bdd;

import com.fca.algorithm.BitSetCbo;
import com.fca.algorithm.CboAlgorithm;
import com.fca.algorithm.CollectionCbo;
import com.fca.implication.ImplicationGenerator;
import com.fca.io.JsonDataLoader;
import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import com.fca.model.Implication;
import com.fca.service.ComparisonResult;
import com.fca.service.ComparisonService;
import com.fca.service.SystemTimingService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AlgorithmComparisonSteps {

    private FormalContext context;
    private ComparisonResult comparisonResult;
    private ComparisonService comparisonService;
    private final ImplicationGenerator implicationGenerator = new ImplicationGenerator();

    private DetailedProfile lastProfile;
    private List<DetailedProfile> preliminaryProfiles;
    private List<ProfilingRow> profilingResults;

    private final Map<String, SavedResult> savedResults = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════
    // GIVEN-шаги
    // ═══════════════════════════════════════════════════════════════

    @Допустим("система сравнения алгоритмов инициализирована")
    public void система_инициализирована() {
        comparisonService = new ComparisonService(
                new SystemTimingService(), implicationGenerator);
    }

    @Допустим("загружен контекст из файла {string}")
    public void загружен_контекст(String filename) throws Exception {
        JsonDataLoader loader = new JsonDataLoader();
        InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        assertNotNull(is, "Ресурс не найден: " + filename);
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            context = loader.parse(reader);
        }
    }

    @Допустим("сгенерирован случайный контекст размером {int}x{int} с плотностью {string}")
    public void сгенерирован_случайный_контекст(int nObj, int nAttr, String densityStr) {
        double density = Double.parseDouble(densityStr);
        context = generateRandomContext(nObj, nAttr, density, 42L);
    }

    @Допустим("загружен датасет {string}")
    public void загружен_датасет(String filename) throws Exception {
        JsonDataLoader loader = new JsonDataLoader();
        InputStream is = getClass().getClassLoader().getResourceAsStream("datasets/" + filename);
        assertNotNull(is, "Датасет не найден в classpath: datasets/" + filename);
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            context = loader.parse(reader);
        }
        System.out.printf("[ДАТАСЕТ] Загружен: %s (%d×%d)%n",
                filename, context.getObjectCount(), context.getAttributeCount());
    }

    @Допустим("подготовлены датасеты для предварительного эксперимента:")
    public void подготовлены_датасеты_для_предварительного(DataTable table) throws Exception {
        preliminaryProfiles = new ArrayList<>();
        JsonDataLoader loader = new JsonDataLoader();
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            String code = row.get("код");
            String filename = row.get("файл");

            // Загружаем контекст из файла
            InputStream is = getClass().getClassLoader().getResourceAsStream("datasets/" + filename);
            assertNotNull(is, "Датасет не найден: datasets/" + filename);
            FormalContext ctx;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                ctx = loader.parse(reader);
            }

            DetailedProfile p = runDetailedProfilingForContext(ctx);
            p.code = code;
            preliminaryProfiles.add(p);
        }
    }

    @Допустим("подготовлены датасеты возрастающего размера:")
    public void подготовлены_датасеты_возрастающего(DataTable table) throws Exception {
        profilingResults = new ArrayList<>();
        JsonDataLoader loader = new JsonDataLoader();
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            String filename = row.get("файл");

            InputStream is = getClass().getClassLoader().getResourceAsStream("datasets/" + filename);
            assertNotNull(is, "Датасет не найден: datasets/" + filename);
            FormalContext ctx;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                ctx = loader.parse(reader);
            }

            ComparisonResult result = comparisonService.compare(
                    ctx, new CollectionCbo(), new BitSetCbo());

            ProfilingRow pRow = new ProfilingRow();
            pRow.nObj = ctx.getObjectCount();
            pRow.nAttr = ctx.getAttributeCount();
            pRow.density = estimateDensity(ctx);
            pRow.conceptCount = result.getResult1().getConceptCount();
            pRow.implicationCount = result.getResult1().getImplicationCount();
            pRow.collectionsTimeMs = result.getResult1().getExecutionTimeMs();
            pRow.bitSetTimeMs = result.getResult2().getExecutionTimeMs();
            pRow.conceptsMatch = result.isConceptsMatch();
            pRow.implicationsMatch = result.isImplicationsMatch();
            profilingResults.add(pRow);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // WHEN-шаги
    // ═══════════════════════════════════════════════════════════════

    @Когда("я запускаю сравнение двух реализаций CbO")
    public void запускаю_сравнение() {
        comparisonResult = comparisonService.compare(
                context, new CollectionCbo(), new BitSetCbo());
    }

    @Когда("я запускаю сравнение двух реализаций CbO с детальным профилированием")
    public void запускаю_сравнение_с_профилированием() {
        lastProfile = runDetailedProfiling(
                context.getObjectCount(), context.getAttributeCount(), -1, 42L);
        // Также сохраняем comparisonResult для проверки совпадения
        comparisonResult = new ComparisonResult(
                lastProfile.collectionsResult, lastProfile.bitSetResult,
                lastProfile.conceptsMatch, lastProfile.implicationsMatch);
    }

    @Когда("я последовательно запускаю оба алгоритма с детальным профилированием")
    public void последовательно_с_профилированием() {
        assertNotNull(preliminaryProfiles, "Предварительные профили не заданы");
        assertFalse(preliminaryProfiles.isEmpty());
    }

    @Когда("я последовательно запускаю оба алгоритма на каждом контексте")
    public void последовательно_запускаю() {
        assertNotNull(profilingResults);
        assertFalse(profilingResults.isEmpty());
    }

    @Когда("я выполняю алгоритм {string} и сохраняю результат как {string}")
    public void выполняю_и_сохраняю(String algoName, String key) {
        CboAlgorithm algo = createAlgorithm(algoName);

        long startConcepts = System.nanoTime();
        List<FormalConcept> concepts = algo.computeConcepts(context);
        long endConcepts = System.nanoTime();

        long startImpl = System.nanoTime();
        List<Implication> implications = implicationGenerator.generate(context, concepts);
        long endImpl = System.nanoTime();

        SavedResult sr = new SavedResult();
        sr.concepts = concepts;
        sr.implications = implications;
        sr.conceptTimeNs = endConcepts - startConcepts;
        sr.implicationTimeNs = endImpl - startImpl;
        sr.algoName = algoName;
        savedResults.put(key, sr);
    }

    // ═══════════════════════════════════════════════════════════════
    // THEN-шаги
    // ═══════════════════════════════════════════════════════════════

    @Тогда("множества найденных понятий совпадают")
    public void понятия_совпадают() {
        assertTrue(comparisonResult.isConceptsMatch(),
                "Множества понятий двух реализаций не совпадают");
    }

    @Тогда("множества найденных импликаций совпадают")
    public void импликации_совпадают() {
        assertTrue(comparisonResult.isImplicationsMatch(),
                "Множества импликаций двух реализаций не совпадают");
    }

    @Тогда("результат сравнения полностью согласован")
    public void результат_согласован() {
        assertTrue(comparisonResult.isFullyConsistent());
    }

    @Тогда("детальный профиль выведен в лог")
    public void детальный_профиль_выведен() {
        assertNotNull(lastProfile, "Детальный профиль не рассчитан");
        printDetailedProfile(lastProfile);
    }

    @Тогда("BitSet-реализация работает не медленнее Collections-реализации на больших данных")
    public void bitset_не_медленнее() {
        double t1 = comparisonResult.getResult1().getExecutionTimeMs();
        double t2 = comparisonResult.getResult2().getExecutionTimeMs();
        assertTrue(t2 <= t1 * 1.2,
                String.format("BitSet (%.3f мс) значительно медленнее Collections (%.3f мс)", t2, t1));
    }

    @Тогда("таблица предварительного эксперимента выведена в лог")
    public void таблица_предварительного() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         ПРЕДВАРИТЕЛЬНЫЙ ЭКСПЕРИМЕНТ — определение максимальных границ                             ║");
        System.out.println("╠═══════╦══════╦══════╦═════╦══════════╦════════════╦═════════════════════════╦═════════════════════════╦════════════╣");
        System.out.println("║ Код   ║ |G|  ║ |M|  ║ d   ║ Понятий  ║ Имплик.   ║  Collections (мс)      ║  BitSet (мс)            ║ Ускорение  ║");
        System.out.println("║       ║      ║      ║     ║          ║           ║ conc.   impl.   total   ║ conc.   impl.   total   ║   k        ║");
        System.out.println("╠═══════╬══════╬══════╬═════╬══════════╬════════════╬═════════════════════════╬═════════════════════════╬════════════╣");

        for (DetailedProfile p : preliminaryProfiles) {
            double collTotal = p.collectionsConceptsMs + p.collectionsImplicationsMs;
            double bsTotal = p.bitSetConceptsMs + p.bitSetImplicationsMs;
            double speedup = bsTotal > 0 ? collTotal / bsTotal : 0;
            System.out.printf("║ %-5s ║ %4d ║ %4d ║ %.1f ║ %8d ║ %9d ║ %7.1f %7.1f %7.1f ║ %7.1f %7.1f %7.1f ║ %8.2fx  ║%n",
                    p.code, p.nObj, p.nAttr, p.density, p.conceptCount, p.implicationCount,
                    p.collectionsConceptsMs, p.collectionsImplicationsMs, collTotal,
                    p.bitSetConceptsMs, p.bitSetImplicationsMs, bsTotal, speedup);
        }

        System.out.println("╠═══════╩══════╩══════╩═════╩══════════╩════════════╩═════════════════════════╩═════════════════════════╩════════════╣");
        // Вывод времени генерации данных и потребления памяти
        System.out.println("║                             Дополнительные метрики                                                                 ║");
        System.out.println("╠═══════╦══════════════════╦═══════════════════╗                                                                     ║");
        System.out.println("║ Код   ║ Генерация (мс)   ║ Память (МБ)       ║                                                                    ║");
        System.out.println("╠═══════╬══════════════════╬═══════════════════╣                                                                     ║");
        for (DetailedProfile p : preliminaryProfiles) {
            System.out.printf("║ %-5s ║ %14.3f   ║ %15.2f   ║%n", p.code, p.dataGenMs, p.memoryMb);
        }
        System.out.println("╚═══════╩══════════════════╩═══════════════════╝                                                                     ");
        System.out.println();
    }

    @Тогда("определена граница максимального размера")
    public void граница_определена() {
        assertNotNull(preliminaryProfiles);
        assertFalse(preliminaryProfiles.isEmpty());

        DetailedProfile largest = preliminaryProfiles.get(preliminaryProfiles.size() - 1);
        double maxTotalMs = largest.collectionsConceptsMs + largest.collectionsImplicationsMs;

        System.out.println("┌──────────────────────────────────────────────────────────────────┐");
        System.out.printf("│ Максимальный протестированный размер: %s (%dx%d, d=%.1f)%n",
                largest.code, largest.nObj, largest.nAttr, largest.density);
        System.out.printf("│ Время Collections: %.1f мс (%.2f сек)%n", maxTotalMs, maxTotalMs / 1000);
        System.out.printf("│ Понятий: %d, Импликаций: %d%n", largest.conceptCount, largest.implicationCount);

        if (preliminaryProfiles.size() >= 2) {
            DetailedProfile prev = preliminaryProfiles.get(preliminaryProfiles.size() - 2);
            double prevTotal = prev.collectionsConceptsMs + prev.collectionsImplicationsMs;
            if (prevTotal > 0) {
                double growthFactor = maxTotalMs / prevTotal;
                System.out.printf("│ Коэффициент роста между последними двумя: %.2fx%n", growthFactor);

                double targetMs = 120_000;
                if (maxTotalMs > 0 && maxTotalMs < targetMs) {
                    double stepsTo2Min = Math.log(targetMs / maxTotalMs) / Math.log(growthFactor);
                    System.out.printf("│ Оценка: ещё ~%.0f шагов увеличения до порога 2 минуты%n", Math.ceil(stepsTo2Min));
                    int estAttr = largest.nAttr + (int) Math.ceil(stepsTo2Min);
                    int estObj = largest.nObj + (int) Math.ceil(stepsTo2Min);
                    System.out.printf("│ Прогнозный «максимальный» размер: ~%dx%d @d=%.1f%n", estObj, estAttr, largest.density);
                } else if (maxTotalMs >= targetMs) {
                    System.out.printf("│ Порог 2 минуты УЖЕ ДОСТИГНУТ на текущем размере!%n");
                }
            }
        }
        System.out.println("│ (Для достижения порога t₁ ∈ [2,10] мин. увеличивайте размер)");
        System.out.println("└──────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Проверка: хотя бы на последнем размере концепты >= 1
        assertTrue(largest.conceptCount > 0, "На максимальном размере должны быть понятия");
    }

    @Тогда("результат {string} совпадает с прямым запуском BitSet")
    public void совпадает_с_unit_bitset(String key) {
        SavedResult bdd = savedResults.get(key);
        assertNotNull(bdd, "Не найден сохранённый результат: " + key);

        // Прямой запуск — эмуляция unit-теста
        BitSetCbo algo = new BitSetCbo();
        List<FormalConcept> unitConcepts = algo.computeConcepts(context);
        List<Implication> unitImplications = implicationGenerator.generate(context, unitConcepts);

        assertEquals(new HashSet<>(unitConcepts), new HashSet<>(bdd.concepts),
                "Понятия BDD и unit-теста не совпадают для BitSet");
        assertEquals(new HashSet<>(unitImplications), new HashSet<>(bdd.implications),
                "Импликации BDD и unit-теста не совпадают для BitSet");
    }

    @Тогда("результат {string} совпадает с прямым запуском Collections")
    public void совпадает_с_unit_collections(String key) {
        SavedResult bdd = savedResults.get(key);
        assertNotNull(bdd, "Не найден сохранённый результат: " + key);

        CollectionCbo algo = new CollectionCbo();
        List<FormalConcept> unitConcepts = algo.computeConcepts(context);
        List<Implication> unitImplications = implicationGenerator.generate(context, unitConcepts);

        assertEquals(new HashSet<>(unitConcepts), new HashSet<>(bdd.concepts),
                "Понятия BDD и unit-теста не совпадают для Collections");
        assertEquals(new HashSet<>(unitImplications), new HashSet<>(bdd.implications),
                "Импликации BDD и unit-теста не совпадают для Collections");
    }

    @Тогда("сводка сравнения BDD vs Unit выведена в лог")
    public void сводка_bdd_vs_unit() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║           Сравнение результатов BDD-тестов и Unit-тестов               ║");
        System.out.println("╠══════════════════════╦════════════════╦════════════════╦═══════════════╣");
        System.out.println("║ Алгоритм             ║ BDD (мс)       ║ Unit (мс)      ║ Совпадение    ║");
        System.out.println("╠══════════════════════╬════════════════╬════════════════╬═══════════════╣");

        for (Map.Entry<String, SavedResult> entry : savedResults.entrySet()) {
            SavedResult sr = entry.getValue();
            double bddMs = (sr.conceptTimeNs + sr.implicationTimeNs) / 1_000_000.0;

            CboAlgorithm algo = createAlgorithm(sr.algoName);
            long unitStart = System.nanoTime();
            List<FormalConcept> unitConcepts = algo.computeConcepts(context);
            implicationGenerator.generate(context, unitConcepts);
            long unitEnd = System.nanoTime();
            double unitMs = (unitEnd - unitStart) / 1_000_000.0;

            boolean match = new HashSet<>(unitConcepts).equals(new HashSet<>(sr.concepts));

            System.out.printf("║ %-20s ║ %12.3f   ║ %12.3f   ║ %-13s ║%n",
                    sr.algoName, bddMs, unitMs, match ? "ДА ✓" : "НЕТ ✗");
        }

        System.out.println("╚══════════════════════╩════════════════╩════════════════╩═══════════════╝");
        System.out.println();
    }

    @Тогда("для каждого контекста множества понятий совпадают")
    public void для_каждого_понятия_совпадают() {
        for (ProfilingRow row : profilingResults) {
            assertTrue(row.conceptsMatch,
                    String.format("Понятия не совпадают для контекста %dx%d", row.nObj, row.nAttr));
        }
    }

    @Тогда("таблица результатов профилирования выведена в лог")
    public void таблица_профилирования() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              Результаты профилирования производительности CbO                      ║");
        System.out.println("╠══════╦══════╦═════════╦══════════╦════════════╦══════════════╦═══════════╦══════════╣");
        System.out.println("║ Obj  ║ Attr ║ Density ║ Concepts ║ Implic.   ║ Collect.(мс) ║ BitS.(мс) ║ Speedup ║");
        System.out.println("╠══════╬══════╬═════════╬══════════╬════════════╬══════════════╬═══════════╬══════════╣");
        for (ProfilingRow r : profilingResults) {
            double speedup = r.bitSetTimeMs > 0 ? r.collectionsTimeMs / r.bitSetTimeMs : 0;
            System.out.printf("║ %4d ║ %4d ║  %.1f    ║ %8d ║ %10d ║ %12.3f ║ %9.3f ║ %7.2fx ║%n",
                    r.nObj, r.nAttr, r.density, r.conceptCount, r.implicationCount,
                    r.collectionsTimeMs, r.bitSetTimeMs, speedup);
        }
        System.out.println("╚══════╩══════╩═════════╩══════════╩════════════╩══════════════╩═══════════╩══════════╝");
        System.out.println();
    }


    private DetailedProfile runDetailedProfiling(int nObj, int nAttr, double density, long seed) {
        FormalContext ctx = density < 0 ? context : generateRandomContext(nObj, nAttr, density, seed);
        return runDetailedProfilingForContext(ctx);
    }
    private DetailedProfile runDetailedProfilingForContext(FormalContext ctx) {
        DetailedProfile p = new DetailedProfile();
        p.nObj = ctx.getObjectCount();
        p.nAttr = ctx.getAttributeCount();
        p.density = estimateDensity(ctx);
        p.dataGenMs = 0; // контекст уже загружен

        Runtime rt = Runtime.getRuntime();
        rt.gc();
        long memBefore = rt.totalMemory() - rt.freeMemory();

        // 1. Collections: вычисление понятий
        CollectionCbo collAlgo = new CollectionCbo();
        long collConceptStart = System.nanoTime();
        List<FormalConcept> collConcepts = collAlgo.computeConcepts(ctx);
        long collConceptEnd = System.nanoTime();
        p.collectionsConceptsMs = (collConceptEnd - collConceptStart) / 1_000_000.0;

        // 2. Collections: генерация импликаций
        long collImplStart = System.nanoTime();
        List<Implication> collImplications = implicationGenerator.generate(ctx, collConcepts);
        long collImplEnd = System.nanoTime();
        p.collectionsImplicationsMs = (collImplEnd - collImplStart) / 1_000_000.0;

        // 3. BitSet: вычисление понятий
        BitSetCbo bsAlgo = new BitSetCbo();
        long bsConceptStart = System.nanoTime();
        List<FormalConcept> bsConcepts = bsAlgo.computeConcepts(ctx);
        long bsConceptEnd = System.nanoTime();
        p.bitSetConceptsMs = (bsConceptEnd - bsConceptStart) / 1_000_000.0;

        // 4. BitSet: генерация импликаций
        long bsImplStart = System.nanoTime();
        List<Implication> bsImplications = implicationGenerator.generate(ctx, bsConcepts);
        long bsImplEnd = System.nanoTime();
        p.bitSetImplicationsMs = (bsImplEnd - bsImplStart) / 1_000_000.0;

        // Потребление памяти
        long memAfter = rt.totalMemory() - rt.freeMemory();
        p.memoryMb = Math.max(0, (memAfter - memBefore)) / (1024.0 * 1024.0);

        p.conceptCount = collConcepts.size();
        p.implicationCount = collImplications.size();
        p.conceptsMatch = new HashSet<>(collConcepts).equals(new HashSet<>(bsConcepts));
        p.implicationsMatch = new HashSet<>(collImplications).equals(new HashSet<>(bsImplications));

        long collTotalNs = (long) ((p.collectionsConceptsMs + p.collectionsImplicationsMs) * 1_000_000);
        long bsTotalNs = (long) ((p.bitSetConceptsMs + p.bitSetImplicationsMs) * 1_000_000);
        p.collectionsResult = new com.fca.model.AnalysisResult(
                collConcepts, collImplications, p.nObj, p.nAttr, collTotalNs, collAlgo.getName());
        p.bitSetResult = new com.fca.model.AnalysisResult(
                bsConcepts, bsImplications, p.nObj, p.nAttr, bsTotalNs, bsAlgo.getName());

        return p;
    }

    private void printDetailedProfile(DetailedProfile p) {
      double collTotal = p.collectionsConceptsMs + p.collectionsImplicationsMs;
      double bsTotal = p.bitSetConceptsMs + p.bitSetImplicationsMs;
      double speedup = bsTotal > 0 ? collTotal / bsTotal : 0;

      System.out.println();
      System.out.println("┌───────────────────────────────────────────────────────────────────┐");
      System.out.printf("│ Контекст: %d × %d, плотность: %.0f%%%n", p.nObj, p.nAttr, p.density * 100);
      System.out.printf("│ Найдено понятий: %d, импликаций: %d%n", p.conceptCount, p.implicationCount);
      System.out.println("├───────────────────────────────────────────────────────────────────┤");
      System.out.printf("│ Генерация данных:          %12.3f мс%n", p.dataGenMs);
      System.out.println("├───────────────────────────────────────────────────────────────────┤");
      System.out.printf("│ Collections — понятия:     %12.3f мс%n", p.collectionsConceptsMs);
      System.out.printf("│ Collections — импликации:  %12.3f мс%n", p.collectionsImplicationsMs);
      System.out.printf("│ Collections — итого:       %12.3f мс%n", collTotal);
      System.out.println("├───────────────────────────────────────────────────────────────────┤");
      System.out.printf("│ BitSet — понятия:          %12.3f мс%n", p.bitSetConceptsMs);
      System.out.printf("│ BitSet — импликации:       %12.3f мс%n", p.bitSetImplicationsMs);
      System.out.printf("│ BitSet — итого:            %12.3f мс%n", bsTotal);
      System.out.println("├───────────────────────────────────────────────────────────────────┤");
      System.out.printf("│ Ускорение BitSet (k):      %12.2fx%n", speedup);
      System.out.printf("│ Потребление памяти:        %12.2f МБ%n", p.memoryMb);
      System.out.printf("│ Понятия совпадают:         %-12s%n", p.conceptsMatch ? "ДА ✓" : "НЕТ ✗");
      System.out.printf("│ Импликации совпадают:      %-12s%n", p.implicationsMatch ? "ДА ✓" : "НЕТ ✗");
      System.out.println("└───────────────────────────────────────────────────────────────────┘");
      System.out.println();
    }

    // утилиты

    static FormalContext generateRandomContext(int nObj, int nAttr, double density, long seed) {
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

    private static double estimateDensity(FormalContext ctx) {
        int total = ctx.getObjectCount() * ctx.getAttributeCount();
        if (total == 0) return 0;
        int trueCount = 0;
        for (int i = 0; i < ctx.getObjectCount(); i++)
            for (int j = 0; j < ctx.getAttributeCount(); j++)
                if (ctx.hasRelation(i, j)) trueCount++;
        return (double) trueCount / total;
    }

    private static CboAlgorithm createAlgorithm(String name) {
        return switch (name) {
            case "CbO (BitSet)" -> new BitSetCbo();
            case "CbO (Collections)" -> new CollectionCbo();
            default -> throw new IllegalArgumentException("Неизвестный алгоритм: " + name);
        };
    }


    private static class DetailedProfile {
        String code;
        int nObj, nAttr, conceptCount, implicationCount;
        double density;
        double dataGenMs;
        double collectionsConceptsMs, collectionsImplicationsMs;
        double bitSetConceptsMs, bitSetImplicationsMs;
        double memoryMb;
        boolean conceptsMatch, implicationsMatch;
        com.fca.model.AnalysisResult collectionsResult, bitSetResult;
    }

    private static class ProfilingRow {
        int nObj, nAttr, conceptCount, implicationCount;
        double density, collectionsTimeMs, bitSetTimeMs;
        boolean conceptsMatch, implicationsMatch;
    }

    private static class SavedResult {
        String algoName;
        List<FormalConcept> concepts;
        List<Implication> implications;
        long conceptTimeNs, implicationTimeNs;
    }
}
