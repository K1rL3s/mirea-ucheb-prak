package ru.mirea.carsharing.domain.billing;

import ru.mirea.carsharing.domain.geo.Zone;
import ru.mirea.carsharing.domain.rental.Rental;

public final class LowFuelRule implements SurchargeRule {
    @Override
    public String typeName() {
        return "Пустой бак";
    }

    @Override
    public boolean applies(Rental rental, Zone finishZone) {
        return rental.hasLowFuel();
    }
}
