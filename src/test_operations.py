from decimal import Decimal

import pytest

from main import DATA_FILE, finish_rental, load_data
from operations import (
    Row,
    calc_trip_cost,
    check_finish_zone,
    check_inspection,
    set_car_status,
    zone_polygons,
)


@pytest.fixture
def data() -> dict[str, list[Row]]:
    return load_data(DATA_FILE)


def test_inspection_lists_missing_required_views(data: dict[str, list[Row]]) -> None:
    assert check_inspection(4, data["photo"], data["photo_view"]) == ["Сзади", "Салон"]
    assert check_inspection(1, data["photo"], data["photo_view"]) == []


@pytest.mark.parametrize(
    ("lat", "lon", "zone_id"),
    [
        (55.7558, 37.6173, 1),
        (55.968, 37.415, 2),
        (55.975, 37.42, None),
        (55.6512, 37.412, None),
    ],
)
def test_finish_zone_handles_concave_polygon(
    data: dict[str, list[Row]],
    lat: float,
    lon: float,
    zone_id: int | None,
) -> None:
    polygons = zone_polygons(data["zone"], data["zone_point"])
    assert check_finish_zone(lat, lon, polygons) == zone_id


def test_trip_cost_rounds_minutes_up(data: dict[str, list[Row]]) -> None:
    trip = calc_trip_cost(
        data["rental"][0],
        data["rental_segment"],
        data["tariff"][0],
        data["surcharge_type"],
        1,
    )
    assert (trip["drive_min"], trip["wait_min"]) == (39, 8)
    assert trip["cost"] == Decimal("500.10")
    assert trip["surcharges"] == []


def test_trip_cost_adds_zone_and_fuel_surcharges(data: dict[str, list[Row]]) -> None:
    trip = calc_trip_cost(
        data["rental"][1],
        data["rental_segment"],
        data["tariff"][0],
        data["surcharge_type"],
        None,
    )
    assert trip["cost"] == Decimal("1311.70")
    assert [s["surcharge_type_id"] for s in trip["surcharges"]] == [1, 2]


def test_car_status_returns_new_record(data: dict[str, list[Row]]) -> None:
    car = data["car"][3]
    assert set_car_status(car, "забронирован")["status"] == "забронирован"
    assert car["status"] == "свободен"


def test_car_status_rejects_forbidden_transition(data: dict[str, list[Row]]) -> None:
    with pytest.raises(ValueError, match="недопустим"):
        set_car_status(data["car"][3], "в аренде")


@pytest.mark.parametrize(
    ("lat", "lon"),
    [(0.0, 1.0), (2.0, 1.0), (1.0, 0.0), (1.0, 2.0), (2.0, 2.0)],
)
def test_finish_zone_includes_boundary(lat: float, lon: float) -> None:
    polygons = [(1, [(0.0, 0.0), (0.0, 2.0), (2.0, 2.0), (2.0, 0.0)])]
    assert check_finish_zone(lat, lon, polygons) == 1


@pytest.mark.parametrize(
    ("mode", "finished_at"),
    [("движение", "2026-10-20T08:51:00"), ("стоянка", "2026-10-20T09:15:00")],
)
def test_trip_cost_rejects_invalid_segment(
    data: dict[str, list[Row]],
    mode: str,
    finished_at: str,
) -> None:
    segment = {**data["rental_segment"][0], "mode": mode, "finished_at": finished_at}
    with pytest.raises(ValueError, match="Некорректный сегмент"):
        calc_trip_cost(data["rental"][0], [segment], data["tariff"][0], [], 1)


def test_finish_rental_handles_absent_inspection(data: dict[str, list[Row]]) -> None:
    data["inspection"] = []
    lines = finish_rental(data, data["rental"][0], [])
    assert (
        "  Фотоосмотр при сдаче: не хватает: Спереди, Сзади, Слева, Справа, Салон"
        in lines
    )
    assert lines[-1] == "  Статус автомобиля: в аренде -> на обслуживании"
