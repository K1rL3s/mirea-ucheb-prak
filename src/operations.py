import math
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Any

Row = dict[str, Any]
Polygon = tuple[int, list[tuple[float, float]]]

CAR_TRANSITIONS = frozenset(
    {
        ("свободен", "забронирован"),
        ("забронирован", "свободен"),
        ("забронирован", "в аренде"),
        ("в аренде", "свободен"),
        ("в аренде", "на обслуживании"),
        ("на обслуживании", "свободен"),
    },
)
LOW_FUEL_PERCENT = 10
OUTSIDE_ZONE = "Завершение вне зоны"
LOW_FUEL = "Пустой бак"


def check_inspection(
    inspection_id: int | None,
    photos: list[Row],
    views: list[Row],
) -> list[str]:
    taken = {p["view_id"] for p in photos if p["inspection_id"] == inspection_id}
    return [v["name"] for v in views if v["required"] and v["view_id"] not in taken]


def zone_polygons(zones: list[Row], zone_points: list[Row]) -> list[Polygon]:
    polygons = []
    for zone in zones:
        points = sorted(
            (p for p in zone_points if p["zone_id"] == zone["zone_id"]),
            key=lambda p: p["seq_no"],
        )
        polygons.append((zone["zone_id"], [(p["lat"], p["lon"]) for p in points]))
    return polygons


def check_finish_zone(lat: float, lon: float, polygons: list[Polygon]) -> int | None:
    for zone_id, vertices in polygons:
        inside = False
        j = len(vertices) - 1
        for i in range(len(vertices)):
            lat_i, lon_i = vertices[i]
            lat_j, lon_j = vertices[j]
            cross = (lat - lat_i) * (lon_j - lon_i) - (lon - lon_i) * (lat_j - lat_i)
            if (
                math.isclose(cross, 0.0, abs_tol=1e-12)
                and min(lat_i, lat_j) <= lat <= max(lat_i, lat_j)
                and min(lon_i, lon_j) <= lon <= max(lon_i, lon_j)
            ):
                return zone_id
            if (lat_i > lat) != (lat_j > lat):
                cross_lon = lon_i + (lat - lat_i) * (lon_j - lon_i) / (lat_j - lat_i)
                if lon < cross_lon:
                    inside = not inside
            j = i
        if inside:
            return zone_id
    return None


def calc_trip_cost(
    rental: Row,
    segments: list[Row],
    tariff: Row,
    surcharge_types: list[Row],
    zone_id: int | None,
) -> Row:
    drive_min = 0
    wait_min = 0
    for s in segments:
        if s["rental_id"] != rental["rental_id"]:
            continue
        duration = datetime.fromisoformat(s["finished_at"]) - datetime.fromisoformat(
            s["started_at"],
        )
        if duration < timedelta(0) or s["mode"] not in {"движение", "ожидание"}:
            raise ValueError(f"Некорректный сегмент аренды: {s['segment_id']}")
        minutes = math.ceil(duration.total_seconds() / 60)
        if s["mode"] == "движение":
            drive_min += minutes
        else:
            wait_min += minutes
    cost = drive_min * Decimal(tariff["drive_price"]) + wait_min * Decimal(
        tariff["wait_price"],
    )
    surcharges = []
    for t in surcharge_types:
        if (t["name"] == OUTSIDE_ZONE and zone_id is None) or (
            t["name"] == LOW_FUEL and rental["fuel_end"] < LOW_FUEL_PERCENT
        ):
            surcharges.append(
                {
                    "rental_id": rental["rental_id"],
                    "surcharge_type_id": t["surcharge_type_id"],
                    "amount": t["amount"],
                },
            )
            cost += Decimal(t["amount"])
    return {
        "drive_min": drive_min,
        "wait_min": wait_min,
        "cost": cost,
        "surcharges": surcharges,
    }


def set_car_status(car: Row, new_status: str) -> Row:
    if (car["status"], new_status) not in CAR_TRANSITIONS:
        raise ValueError(
            f"Автомобиль {car['plate']}: переход "
            f"«{car['status']}» -> «{new_status}» недопустим",
        )
    return {**car, "status": new_status}
