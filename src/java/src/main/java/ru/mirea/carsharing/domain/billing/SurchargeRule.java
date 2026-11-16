package ru.mirea.carsharing.domain.billing;

import ru.mirea.carsharing.domain.geo.Zone;
import ru.mirea.carsharing.domain.rental.Rental;

public interface SurchargeRule {
    String typeName();
    boolean applies(Rental rental, Zone finishZone);
}
