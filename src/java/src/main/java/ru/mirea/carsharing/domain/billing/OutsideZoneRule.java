package ru.mirea.carsharing.domain.billing;

import ru.mirea.carsharing.domain.geo.Zone;
import ru.mirea.carsharing.domain.rental.Rental;

public final class OutsideZoneRule implements SurchargeRule {
    @Override
    public String typeName() {
        return "Завершение вне зоны";
    }

    @Override
    public boolean applies(Rental rental, Zone finishZone) {
        return finishZone == null;
    }
}
