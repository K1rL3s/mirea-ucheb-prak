package ru.mirea.carsharing.domain.rental;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Claim(
        int id, int rentalId, String description, BigDecimal amount, String status, LocalDateTime createdAt
) {}
