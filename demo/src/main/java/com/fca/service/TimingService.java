package com.fca.service;

/**
 * Интерфейс сервиса измерения времени.
 * Абстракция для возможности мокирования в тестах.
 */
public interface TimingService {
    long currentTimeNanos();
}
