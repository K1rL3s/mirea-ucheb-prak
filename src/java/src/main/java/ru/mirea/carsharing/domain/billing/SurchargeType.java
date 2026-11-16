package ru.mirea.carsharing.domain.billing;

import java.math.BigDecimal;

public record SurchargeType(int id, String name, BigDecimal amount) {}
