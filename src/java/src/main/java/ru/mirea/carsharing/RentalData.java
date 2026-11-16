package ru.mirea.carsharing;

import ru.mirea.carsharing.domain.billing.Payment;
import ru.mirea.carsharing.domain.billing.Surcharge;
import ru.mirea.carsharing.domain.billing.SurchargeType;
import ru.mirea.carsharing.domain.geo.Zone;
import ru.mirea.carsharing.domain.inspection.PhotoView;
import ru.mirea.carsharing.domain.rental.Claim;
import ru.mirea.carsharing.domain.rental.Rental;

import java.util.List;

record RentalData(
        List<Rental> rentals, List<Zone> zones, List<PhotoView> views, List<SurchargeType> surchargeTypes,
        List<Surcharge> recordedSurcharges, List<Payment> payments, List<Claim> claims
) {}
