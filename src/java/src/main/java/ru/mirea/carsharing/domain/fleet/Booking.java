package ru.mirea.carsharing.domain.fleet;

import java.time.LocalDateTime;

public record Booking(
        int id, Client client, Car car, LocalDateTime createdAt, LocalDateTime expiresAt, String status
) {}
