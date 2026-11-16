package ru.mirea.carsharing.domain.geo;

import java.util.List;

public record Zone(int id, String name, List<ZonePoint> points) {
    public boolean contains(double lat, double lon) {
        boolean inside = false;
        var previous = points.getLast();
        for (var current : points) {
            double latDiff = previous.lat() - current.lat();
            double lonDiff = previous.lon() - current.lon();
            double cross = (lat - current.lat()) * lonDiff - (lon - current.lon()) * latDiff;
            if (Math.abs(cross) <= 1e-12
                    && between(lat, current.lat(), previous.lat())
                    && between(lon, current.lon(), previous.lon())) {
                return true;
            }
            if ((current.lat() > lat) != (previous.lat() > lat)) {
                double crossLon = current.lon() + (lat - current.lat()) * lonDiff / latDiff;
                if (lon < crossLon) {
                    inside = !inside;
                }
            }
            previous = current;
        }
        return inside;
    }

    private static boolean between(double value, double a, double b) {
        return Math.min(a, b) <= value && value <= Math.max(a, b);
    }
}
