package ru.mirea.carsharing.domain.rental;

import ru.mirea.carsharing.domain.billing.BillableMinutes;

import java.time.Duration;
import java.time.LocalDateTime;

public sealed abstract class RentalSegment permits DrivingSegment, WaitingSegment {
    private final int id;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;

    protected RentalSegment(int id, LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.id = id;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public final BillableMinutes billableMinutes() {
        Duration duration = Duration.between(startedAt, finishedAt);
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Некорректный сегмент аренды: " + id);
        }
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        if (seconds % 60 != 0 || duration.getNano() != 0) {
            minutes++;
        }
        return splitMinutes(minutes);
    }

    protected abstract BillableMinutes splitMinutes(long minutes);
}
