package com.fca.io;

import com.fca.model.AnalysisResult;

public interface ResultWriter {

    /**
     * Записать результат анализа по указанному пути.
     *
     * @param result результат анализа
     * @param destination путь к выходному файлу
     * @throws Exception если запись не удалась
     */
    void write(AnalysisResult result, String destination) throws Exception;
}
