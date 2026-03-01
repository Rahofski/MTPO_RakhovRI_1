package com.fca.implication;

import com.fca.model.FormalConcept;
import com.fca.model.FormalContext;
import com.fca.model.Implication;

import java.util.*;

/**
 * Генератор замкнутых импликативных правил.
 * Для каждого замкнутого множества атрибутов (интента понятия) и каждого
 * атрибута, не входящего в него, вычисляет замыкание объединения и строит
 * правило вида (intent ∪ {attr}) → (closure \ (intent ∪ {attr})).
 */
public class ImplicationGenerator {

    /**
     * Генерирует список замкнутых импликативных правил из набора формальных понятий.
     *
     * @param context  формальный контекст
     * @param concepts список формальных понятий
     * @return список импликаций (без дубликатов)
     */
    public List<Implication> generate(FormalContext context, List<FormalConcept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Implication> implSet = new LinkedHashSet<>();
        Set<Set<String>> processedPremises = new HashSet<>();

        // Собираем все интенты + пустое множество как базы для генерации
        List<Set<String>> bases = new ArrayList<>();
        bases.add(new TreeSet<>()); // пустое множество
        for (FormalConcept concept : concepts) {
            bases.add(concept.getIntent());
        }

        for (Set<String> intent : bases) {

            for (String attr : context.getAttributes()) {
                if (!intent.contains(attr)) {
                    Set<String> premise = new TreeSet<>(intent);
                    premise.add(attr);

                    // Пропускаем, если эта посылка уже обработана
                    if (processedPremises.contains(premise)) {
                        continue;
                    }
                    processedPremises.add(premise);

                    // Вычисляем замыкание посылки
                    Set<String> closure = context.computeClosure(premise);

                    // Заключение: атрибуты, добавленные замыканием
                    Set<String> conclusion = new TreeSet<>(closure);
                    conclusion.removeAll(premise);

                    if (!conclusion.isEmpty()) {
                        implSet.add(new Implication(premise, conclusion));
                    }
                }
            }
        }

        return new ArrayList<>(implSet);
    }
}
