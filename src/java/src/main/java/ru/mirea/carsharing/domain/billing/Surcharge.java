package ru.mirea.carsharing.domain.billing;

import java.math.BigDecimal;

public record Surcharge(int rentalId, SurchargeType type, BigDecimal amount) {}
