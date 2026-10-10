package com.nerlogix.util;

public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the Haversine distance between two lat/lng coordinates in kilometers.
     */
    public static double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Checks if a point is within a bounding box (minLat, minLng, maxLat, maxLng).
     */
    public static boolean isWithinBbox(double lat, double lng, Double minLat, Double minLng, Double maxLat, Double maxLng) {
        if (minLat != null && lat < minLat) return false;
        if (maxLat != null && lat > maxLat) return false;
        if (minLng != null && lng < minLng) return false;
        if (maxLng != null && lng > maxLng) return false;
        return true;
    }
}
