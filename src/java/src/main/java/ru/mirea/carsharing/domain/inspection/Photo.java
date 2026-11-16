package ru.mirea.carsharing.domain.inspection;

import java.time.LocalDateTime;

public record Photo(PhotoView view, String fileName, LocalDateTime takenAt) {}
