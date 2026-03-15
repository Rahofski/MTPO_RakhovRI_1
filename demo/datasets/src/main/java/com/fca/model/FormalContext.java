package com.fca.model;

import java.util.*;

/**
 * Формальный контекст (G, M, I): множество объектов G, множество атрибутов M,
 * бинарное отношение инцидентности I ⊆ G × M.
 */
public class FormalContext {

    private final List<String> objects;
    private final List<String> attributes;
    private final boolean[][] incidence;

    public FormalContext(List<String> objects, List<String> attributes, boolean[][] incidence) {
        if (objects == null || attributes == null || incidence == null) {
            throw new IllegalArgumentException("Объекты, атрибуты и матрица инцидентности не могут быть null");
        }
        if (incidence.length != objects.size()) {
            throw new IllegalArgumentException(
                    "Число строк матрицы инцидентности (" + incidence.length +
                            ") не совпадает с числом объектов (" + objects.size() + ")");
        }
        for (int i = 0; i < incidence.length; i++) {
            if (incidence[i] == null || incidence[i].length != attributes.size()) {
                throw new IllegalArgumentException(
                        "Строка " + i + " матрицы инцидентности имеет неверную длину");
            }
        }
        this.objects = Collections.unmodifiableList(new ArrayList<>(objects));
        this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));
        // deep copy
        this.incidence = new boolean[objects.size()][];
        for (int i = 0; i < objects.size(); i++) {
            this.incidence[i] = Arrays.copyOf(incidence[i], incidence[i].length);
        }
    }

    public int getObjectCount() {
        return objects.size();
    }

    public int getAttributeCount() {
        return attributes.size();
    }

    public List<String> getObjects() {
        return objects;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public boolean hasRelation(int objectIndex, int attributeIndex) {
        return incidence[objectIndex][attributeIndex];
    }

    /** B' — множество объектов, обладающих всеми атрибутами из attrSet. */
    public Set<String> computeExtent(Set<String> attrSet) {
        Set<String> extent = new TreeSet<>();
        for (int i = 0; i < objects.size(); i++) {
            boolean hasAll = true;
            for (String attr : attrSet) {
                int j = attributes.indexOf(attr);
                if (j < 0 || !incidence[i][j]) {
                    hasAll = false;
                    break;
                }
            }
            if (hasAll) {
                extent.add(objects.get(i));
            }
        }
        return extent;
    }

    /** A' — множество атрибутов, общих для всех объектов из objSet. */
    public Set<String> computeIntent(Set<String> objSet) {
        Set<String> intent = new TreeSet<>();
        for (int j = 0; j < attributes.size(); j++) {
            boolean allHave = true;
            for (String obj : objSet) {
                int i = objects.indexOf(obj);
                if (i < 0 || !incidence[i][j]) {
                    allHave = false;
                    break;
                }
            }
            if (allHave) {
                intent.add(attributes.get(j));
            }
        }
        return intent;
    }

    /** B'' = (B')' — замыкание множества атрибутов. */
    public Set<String> computeClosure(Set<String> attrSet) {
        return computeIntent(computeExtent(attrSet));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Формальный контекст: ").append(objects.size())
                .append(" объектов × ").append(attributes.size()).append(" атрибутов\n\n");

        // Вычисляем ширину столбца для имён объектов
        int objWidth = 4; // минимум "    "
        for (String obj : objects) {
            objWidth = Math.max(objWidth, obj.length());
        }
        objWidth += 2; // отступ

        // Вычисляем ширину каждого столбца атрибута
        int[] colWidths = new int[attributes.size()];
        for (int j = 0; j < attributes.size(); j++) {
            colWidths[j] = Math.max(attributes.get(j).length(), 1) + 2;
        }

        // Заголовок — имена атрибутов
        sb.append(String.format("%-" + objWidth + "s│", ""));
        for (int j = 0; j < attributes.size(); j++) {
            sb.append(String.format(" %-" + (colWidths[j] - 1) + "s", attributes.get(j)));
        }
        sb.append("\n");

        // Разделитель
        sb.append("─".repeat(objWidth)).append("┼");
        for (int j = 0; j < attributes.size(); j++) {
            sb.append("─".repeat(colWidths[j]));
        }
        sb.append("\n");

        // Строки данных
        for (int i = 0; i < objects.size(); i++) {
            sb.append(String.format("%-" + objWidth + "s│", objects.get(i)));
            for (int j = 0; j < attributes.size(); j++) {
                String mark = incidence[i][j] ? "×" : "·";
                int pad = colWidths[j] / 2;
                sb.append(" ".repeat(pad)).append(mark).append(" ".repeat(colWidths[j] - pad - 1));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
