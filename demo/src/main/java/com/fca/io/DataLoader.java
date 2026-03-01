package com.fca.io;

import com.fca.model.FormalContext;

public interface DataLoader {

    /**
     * Загрузить формальный контекст из указанного источника.
     *
     * @param source путь к файлу или иной идентификатор
     * @return загруженный формальный контекст
     * @throws Exception если загрузка не удалась
     */
    FormalContext load(String source) throws Exception;
}
