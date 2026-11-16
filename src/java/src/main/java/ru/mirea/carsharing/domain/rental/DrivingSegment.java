package ru.mirea.carsharing.domain.rental;

import ru.mirea.carsharing.domain.billing.BillableMinutes;

import java.time.LocalDateTime;

public final class DrivingSegment extends RentalSegment {
    public DrivingSegment(int id, LocalDateTime startedAt, LocalDateTime finishedAt) {
        super(id, startedAt, finishedAt);
    }

    @Override
    protected BillableMinutes splitMinutes(long minutes) {
        return new BillableMinutes(minutes, 0);
    }
}
