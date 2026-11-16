package ru.mirea.carsharing.domain.fleet;

import java.math.BigDecimal;

public record Tariff(int id, String name, BigDecimal drivePrice, BigDecimal waitPrice, int freeWaitMin) {}
