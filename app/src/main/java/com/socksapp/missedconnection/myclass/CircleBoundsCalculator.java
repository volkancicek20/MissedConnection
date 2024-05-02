package com.socksapp.missedconnection.myclass;

import com.google.maps.model.LatLng;

public class CircleBoundsCalculator {

    // Dünya'nın yarıçapı (metre cinsinden)
    private static final double EARTH_RADIUS = 6371000;

    // 1 metre kuzey ve güneydeki bir dereceye eşdeğerdir (Dünya bir küre olduğundan)
    private static final double METERS_PER_DEGREE_LAT = 111319.9;

    // 1 metre doğu ve batıdaki bir dereceye eşdeğerdir (enlemdeki boylam uzunluğu, enlem derecesine bağlı olarak değişir)
    private static double metersPerDegreeLng(double lat) {
        double radians = Math.toRadians(lat);
        return EARTH_RADIUS * Math.cos(radians) * Math.PI / 180;
    }

    public static LatLng[] calculateBounds(LatLng center, double radius) {
        double lat = center.lat;
        double lng = center.lng;

        // Latitude sınırları hesapla
        double latDelta = radius / METERS_PER_DEGREE_LAT;
        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;

        // Longitude sınırları hesapla
        double lngDelta = radius / metersPerDegreeLng(lat);
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        // Sınırları LatLng dizisi olarak döndür
        return new LatLng[] {
                new LatLng(minLat, minLng),
                new LatLng(maxLat, maxLng)
        };
    }

    public static void main(String[] args) {
        // Merkez noktası ve yarıçapı tanımla
        LatLng center = new LatLng(40.7128, -74.0060); // Örnek New York'un koordinatları
        double radius = 100; // 100 metrelik yarıçap

        // Sınırları hesapla
        LatLng[] bounds = calculateBounds(center, radius);

        // Sonuçları yazdır
        System.out.println("Min Lat: " + bounds[0].lat);
        System.out.println("Min Lng: " + bounds[0].lng);
        System.out.println("Max Lat: " + bounds[1].lat);
        System.out.println("Max Lng: " + bounds[1].lng);
    }
}

