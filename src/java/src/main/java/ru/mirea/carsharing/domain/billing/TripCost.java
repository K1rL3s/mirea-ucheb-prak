package ru.mirea.carsharing.domain.billing;

import java.math.BigDecimal;
import java.util.List;

public record TripCost(long driveMinutes, long waitMinutes, BigDecimal total, List<Surcharge> surcharges) {}
