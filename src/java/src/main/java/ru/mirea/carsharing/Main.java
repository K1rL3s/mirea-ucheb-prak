package ru.mirea.carsharing;

import ru.mirea.carsharing.domain.billing.LowFuelRule;
import ru.mirea.carsharing.domain.billing.OutsideZoneRule;
import ru.mirea.carsharing.domain.billing.SurchargeRule;
import ru.mirea.carsharing.domain.rental.Rental;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException {
        var path = Path.of(args.length == 0 ? "data.json" : args[0]);
        var data = new JsonReader().read(path);
        var rules = Stream.<SurchargeRule>of(new OutsideZoneRule(), new LowFuelRule())
                .collect(Collectors.toUnmodifiableMap(SurchargeRule::typeName, Function.identity()));
        for (var rental : data.rentals()) {
            System.out.println(String.join("\n", finishRental(rental, data, rules)));
        }
    }

    private static List<String> finishRental(Rental rental, RentalData data, Map<String, SurchargeRule> rules) {
        var booking = rental.booking();
        var car = booking.car();
        var tariff = car.tariff();
        List<String> lines = new ArrayList<>();
        lines.add("Аренда %d: %s, %s %s, тариф «%s»".formatted(
                rental.id(), booking.client().fullName(), car.model(), car.plate(), tariff.name()));

        var zone = rental.checkFinishZone(data.zones());
        lines.add(zone == null ? "  Место завершения: вне зон завершения"
                : "  Место завершения: зона «%s»".formatted(zone.name()));

        var cost = rental.calculateTripCost(data.surchargeTypes(), rules, zone);
        lines.add("  Движение: %d мин x %s руб.".formatted(cost.driveMinutes(), tariff.drivePrice().toPlainString()));
        lines.add("  Ожидание: %d мин x %s руб.".formatted(cost.waitMinutes(), tariff.waitPrice().toPlainString()));
        for (var surcharge : cost.surcharges()) {
            lines.add("  Доплата «%s»: %s руб.".formatted(surcharge.type().name(), surcharge.amount().toPlainString()));
        }
        lines.add("  Стоимость поездки: %s руб.".formatted(cost.total().toPlainString()));

        var missing = rental.checkInspection(data.views());
        lines.add(missing.isEmpty() ? "  Фотоосмотр при сдаче: полный"
                : "  Фотоосмотр при сдаче: не хватает: " + String.join(", ", missing));
        boolean needsService = !missing.isEmpty() || rental.hasLowFuel();
        var updated = car.withStatus(needsService ? "на обслуживании" : "свободен");
        lines.add("  Статус автомобиля: %s -> %s".formatted(car.status(), updated.status()));
        return lines;
    }
}
