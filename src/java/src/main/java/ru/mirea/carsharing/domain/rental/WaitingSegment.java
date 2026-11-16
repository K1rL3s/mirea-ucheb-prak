package ru.mirea.carsharing.domain.rental;

import ru.mirea.carsharing.domain.billing.BillableMinutes;

import java.time.LocalDateTime;

public final class WaitingSegment extends RentalSegment {
    public WaitingSegment(int id, LocalDateTime startedAt, LocalDateTime finishedAt) {
        super(id, startedAt, finishedAt);
    }

    @Override
    protected BillableMinutes splitMinutes(long minutes) {
        return new BillableMinutes(0, minutes);
    }
}
