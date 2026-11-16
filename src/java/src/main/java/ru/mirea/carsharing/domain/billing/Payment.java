package ru.mirea.carsharing.domain.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(int id, int rentalId, BigDecimal amount, LocalDateTime paidAt, String status) {}
