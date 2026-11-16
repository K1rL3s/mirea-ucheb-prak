package ru.mirea.carsharing.domain.fleet;

import java.util.Map;
import java.util.Set;

public record Car(
        int id, String plate, String model, Tariff tariff, String status, double lat, double lon, double fuelLevel
) {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "свободен", Set.of("забронирован"),
            "забронирован", Set.of("свободен", "в аренде"),
            "в аренде", Set.of("свободен", "на обслуживании"),
            "на обслуживании", Set.of("свободен")
    );

    public Car withStatus(String nextStatus) {
        if (!TRANSITIONS.getOrDefault(status, Set.of()).contains(nextStatus)) {
            throw new IllegalArgumentException(
                    "Автомобиль %s: переход «%s» -> «%s» недопустим".formatted(plate, status, nextStatus));
        }
        return new Car(id, plate, model, tariff, nextStatus, lat, lon, fuelLevel);
    }
}
