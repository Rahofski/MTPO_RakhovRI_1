package com.fca.service;

/**
 * Реализация TimingService на основе System.nanoTime().
 */
public class SystemTimingService implements TimingService {

    @Override
    public long currentTimeNanos() {
        return System.nanoTime();
    }
}
