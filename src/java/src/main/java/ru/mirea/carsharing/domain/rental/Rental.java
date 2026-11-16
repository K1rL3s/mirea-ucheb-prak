package ru.mirea.carsharing.domain.rental;

import ru.mirea.carsharing.domain.billing.Surcharge;
import ru.mirea.carsharing.domain.billing.SurchargeRule;
import ru.mirea.carsharing.domain.billing.SurchargeType;
import ru.mirea.carsharing.domain.billing.TripCost;
import ru.mirea.carsharing.domain.fleet.Booking;
import ru.mirea.carsharing.domain.fleet.Tariff;
import ru.mirea.carsharing.domain.geo.Zone;
import ru.mirea.carsharing.domain.inspection.Inspection;
import ru.mirea.carsharing.domain.inspection.PhotoView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Rental(
        int id, Booking booking, LocalDateTime startedAt, LocalDateTime finishedAt,
        double finishLat, double finishLon, double fuelEnd, Zone recordedZone, BigDecimal recordedCost,
        List<RentalSegment> segments, List<Inspection> inspections
) {
    private static final int LOW_FUEL_PERCENT = 10;

    public Zone checkFinishZone(List<Zone> zones) {
        return zones.stream().filter(zone -> zone.contains(finishLat, finishLon)).findFirst().orElse(null);
    }

    public TripCost calculateTripCost(List<SurchargeType> types, Map<String, SurchargeRule> rules, Zone finishZone) {
        long drive = 0;
        long wait = 0;
        for (var segment : segments) {
            var minutes = segment.billableMinutes();
            drive += minutes.drive();
            wait += minutes.waitMinutes();
        }

        Tariff tariff = booking.car().tariff();
        BigDecimal total = tariff.drivePrice().multiply(BigDecimal.valueOf(drive))
                .add(tariff.waitPrice().multiply(BigDecimal.valueOf(wait)));

        List<Surcharge> surcharges = new ArrayList<>();
        for (var type : types) {
            var rule = rules.get(type.name());
            if (rule != null && rule.applies(this, finishZone)) {
                surcharges.add(new Surcharge(id, type, type.amount()));
                total = total.add(type.amount());
            }
        }
        return new TripCost(drive, wait, total, List.copyOf(surcharges));
    }

    public List<String> checkInspection(List<PhotoView> views) {
        for (var inspection : inspections) {
            if (inspection.stage().equals("сдача")) {
                return inspection.missingViews(views);
            }
        }
        return views.stream().filter(PhotoView::required).map(PhotoView::name).toList();
    }

    public boolean hasLowFuel() {
        return fuelEnd < LOW_FUEL_PERCENT;
    }
}
