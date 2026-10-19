import json
import sys
from pathlib import Path
from typing import Any

from operations import (
    LOW_FUEL_PERCENT,
    Polygon,
    Row,
    calc_trip_cost,
    check_finish_zone,
    check_inspection,
    set_car_status,
    zone_polygons,
)

DATA_FILE = Path(__file__).with_name("data.json")


def load_data(path: Path) -> dict[str, list[Row]]:
    data: dict[str, list[Row]] = json.loads(path.read_text(encoding="utf-8"))
    return data


def by_id(rows: list[Row], key: str) -> dict[Any, Row]:
    return {row[key]: row for row in rows}


def finish_rental(
    data: dict[str, list[Row]],
    rental: Row,
    polygons: list[Polygon],
) -> list[str]:
    booking = by_id(data["booking"], "booking_id")[rental["booking_id"]]
    client = by_id(data["client"], "client_id")[booking["client_id"]]
    car = by_id(data["car"], "car_id")[booking["car_id"]]
    tariff = by_id(data["tariff"], "tariff_id")[car["tariff_id"]]
    lines = [
        (
            f"Аренда {rental['rental_id']}: {client['full_name']}, "
            f"{car['model']} {car['plate']}, тариф «{tariff['name']}»"
        ),
    ]

    returned = next(
        i
        for i in data["inspection"]
        if i["rental_id"] == rental["rental_id"] and i["stage"] == "сдача"
    )
    missing = check_inspection(
        returned["inspection_id"],
        data["photo"],
        data["photo_view"],
    )
    if missing:
        lines.append("  Фотоосмотр при сдаче: не хватает: " + ", ".join(missing))
    else:
        lines.append("  Фотоосмотр при сдаче: полный")

    zone_id = check_finish_zone(rental["finish_lat"], rental["finish_lon"], polygons)
    if zone_id is None:
        lines.append("  Место завершения: вне зон завершения")
    else:
        zone = by_id(data["zone"], "zone_id")[zone_id]
        lines.append(f"  Место завершения: зона «{zone['name']}»")

    trip = calc_trip_cost(
        rental,
        data["rental_segment"],
        tariff,
        data["surcharge_type"],
        zone_id,
    )
    lines.append(f"  Движение: {trip['drive_min']} мин x {tariff['drive_price']} руб.")
    lines.append(f"  Ожидание: {trip['wait_min']} мин x {tariff['wait_price']} руб.")
    types = by_id(data["surcharge_type"], "surcharge_type_id")
    lines.extend(
        f"  Доплата «{types[s['surcharge_type_id']]['name']}»: {s['amount']} руб."
        for s in trip["surcharges"]
    )
    lines.append(f"  Стоимость поездки: {trip['cost']:.2f} руб.")

    needs_service = bool(missing) or rental["fuel_end"] < LOW_FUEL_PERCENT
    updated = set_car_status(car, "на обслуживании" if needs_service else "свободен")
    lines.append(f"  Статус автомобиля: {car['status']} -> {updated['status']}")
    return lines


def main() -> None:
    data = load_data(Path(sys.argv[1]) if len(sys.argv) > 1 else DATA_FILE)
    polygons = zone_polygons(data["zone"], data["zone_point"])
    for rental in data["rental"]:
        sys.stdout.write("\n".join(finish_rental(data, rental, polygons)) + "\n")


if __name__ == "__main__":
    main()
