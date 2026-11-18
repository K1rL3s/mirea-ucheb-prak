package ru.mirea.carsharing;

import ru.mirea.carsharing.domain.billing.Payment;
import ru.mirea.carsharing.domain.billing.Surcharge;
import ru.mirea.carsharing.domain.billing.SurchargeType;
import ru.mirea.carsharing.domain.fleet.Booking;
import ru.mirea.carsharing.domain.fleet.Car;
import ru.mirea.carsharing.domain.fleet.Client;
import ru.mirea.carsharing.domain.fleet.Tariff;
import ru.mirea.carsharing.domain.geo.Zone;
import ru.mirea.carsharing.domain.geo.ZonePoint;
import ru.mirea.carsharing.domain.inspection.Inspection;
import ru.mirea.carsharing.domain.inspection.Photo;
import ru.mirea.carsharing.domain.inspection.PhotoView;
import ru.mirea.carsharing.domain.rental.Claim;
import ru.mirea.carsharing.domain.rental.DrivingSegment;
import ru.mirea.carsharing.domain.rental.Rental;
import ru.mirea.carsharing.domain.rental.RentalSegment;
import ru.mirea.carsharing.domain.rental.WaitingSegment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

final class JsonReader {
    RentalData read(Path path) throws IOException {
        JsonNode data = new ObjectMapper().readTree(path.toFile());
        var clients = index(rows(data, "client", r -> new Client(
                id(r, "client_id"), value(r, "full_name"), value(r, "phone"),
                value(r, "license_number"), value(r, "account_status"))), Client::id);
        var tariffs = index(rows(data, "tariff", r -> new Tariff(
                id(r, "tariff_id"), value(r, "name"), money(r, "drive_price"),
                money(r, "wait_price"), id(r, "free_wait_min"))), Tariff::id);
        var cars = index(rows(data, "car", r -> new Car(
                id(r, "car_id"), value(r, "plate"), value(r, "model"), linked(tariffs, r, "tariff_id"),
                value(r, "status"), number(r, "lat"), number(r, "lon"), number(r, "fuel_level"))), Car::id);
        var bookings = index(rows(data, "booking", r -> new Booking(
                id(r, "booking_id"), linked(clients, r, "client_id"), linked(cars, r, "car_id"),
                time(r, "created_at"), time(r, "expires_at"), value(r, "status"))), Booking::id);

        var points = grouped(data, "zone_point", "zone_id",
                r -> new ZonePoint(id(r, "seq_no"), number(r, "lat"), number(r, "lon")));
        var zones = index(rows(data, "zone", r -> new Zone(
                id(r, "zone_id"), value(r, "name"), points.getOrDefault(id(r, "zone_id"), List.of()).stream()
                        .sorted(Comparator.comparingInt(ZonePoint::seqNo)).toList())), Zone::id);
        var segments = grouped(data, "rental_segment", "rental_id", this::segment);
        var views = index(rows(data, "photo_view", r -> new PhotoView(
                id(r, "view_id"), value(r, "name"), r.required("required").booleanValue())), PhotoView::id);
        var photos = grouped(data, "photo", "inspection_id", r -> new Photo(
                linked(views, r, "view_id"), value(r, "file_name"), time(r, "taken_at")));
        var inspections = grouped(data, "inspection", "rental_id", r -> new Inspection(
                id(r, "inspection_id"), value(r, "stage"), time(r, "created_at"),
                photos.getOrDefault(id(r, "inspection_id"), List.of())));
        var rentals = rows(data, "rental", r -> {
            int rentalId = id(r, "rental_id");
            return new Rental(rentalId, linked(bookings, r, "booking_id"), time(r, "started_at"), time(r, "finished_at"),
                    number(r, "finish_lat"), number(r, "finish_lon"), number(r, "fuel_end"),
                    r.required("zone_id").isNull() ? null : linked(zones, r, "zone_id"), money(r, "cost"),
                    segments.getOrDefault(rentalId, List.of()), inspections.getOrDefault(rentalId, List.of()));
        });

        var types = index(rows(data, "surcharge_type", r -> new SurchargeType(
                id(r, "surcharge_type_id"), value(r, "name"), money(r, "amount"))), SurchargeType::id);
        var surcharges = rows(data, "surcharge", r -> new Surcharge(
                id(r, "rental_id"), linked(types, r, "surcharge_type_id"), money(r, "amount")));
        var payments = rows(data, "payment", r -> new Payment(
                id(r, "payment_id"), id(r, "rental_id"), money(r, "amount"), time(r, "paid_at"), value(r, "status")));
        var claims = rows(data, "claim", r -> new Claim(
                id(r, "claim_id"), id(r, "rental_id"), value(r, "description"), money(r, "amount"),
                value(r, "status"), time(r, "created_at")));
        return new RentalData(rentals, List.copyOf(zones.values()), List.copyOf(views.values()),
                List.copyOf(types.values()), surcharges, payments, claims);
    }

    private RentalSegment segment(JsonNode row) {
        int id = id(row, "segment_id");
        var start = time(row, "started_at");
        var finish = time(row, "finished_at");
        return switch (value(row, "mode")) {
            case "движение" -> new DrivingSegment(id, start, finish);
            case "ожидание" -> new WaitingSegment(id, start, finish);
            default -> throw new IllegalArgumentException("Некорректный сегмент аренды: " + id);
        };
    }

    private <T> List<T> rows(JsonNode data, String key, Function<JsonNode, T> read) {
        List<T> rows = new ArrayList<>();
        for (var row : data.required(key)) {
            rows.add(read.apply(row));
        }
        return List.copyOf(rows);
    }

    private <T> Map<Integer, List<T>> grouped(JsonNode data, String key, String foreignKey, Function<JsonNode, T> read) {
        Map<Integer, List<T>> groups = new LinkedHashMap<>();
        for (var row : data.required(key)) {
            groups.computeIfAbsent(id(row, foreignKey), ignored -> new ArrayList<>()).add(read.apply(row));
        }
        groups.replaceAll((ignored, items) -> List.copyOf(items));
        return groups;
    }

    private <T> Map<Integer, T> index(List<T> values, ToIntFunction<T> id) {
        Map<Integer, T> indexed = new LinkedHashMap<>();
        for (T value : values) {
            indexed.put(id.applyAsInt(value), value);
        }
        return indexed;
    }

    private <T> T linked(Map<Integer, T> values, JsonNode row, String key) {
        int id = id(row, key);
        T item = values.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Не найдена запись %s = %d".formatted(key, id));
        }
        return item;
    }

    private int id(JsonNode row, String field) {
        var value = row.required(field);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException("Поле %s должно быть целым числом в диапазоне int: %s"
                    .formatted(field, value));
        }
        return value.intValue();
    }

    private double number(JsonNode row, String field) {
        return row.required(field).doubleValue();
    }

    private String value(JsonNode row, String field) {
        return row.required(field).asText();
    }

    private BigDecimal money(JsonNode row, String field) {
        var value = row.required(field);
        return value.isNull() ? null : new BigDecimal(value.asText());
    }

    private LocalDateTime time(JsonNode row, String field) {
        var value = row.required(field);
        return value.isNull() ? null : LocalDateTime.parse(value.asText());
    }
}
