package com.socksapp.missedconnection.myclass;

import com.google.maps.model.LatLng;

public class CircleCheck {

    public static boolean isCoordinateInsideCircle(LatLng coordinate, LatLng center, double radius) {
        // İlk koordinatın dairenin sınırlarını belirle
        LatLng[] bounds = calculateBounds(center, radius);
        double minLat = bounds[0].lat;
        double minLng = bounds[0].lng;
        double maxLat = bounds[1].lat;
        double maxLng = bounds[1].lng;

        // İkinci koordinatın koordinatlarını al
        double lat = coordinate.lat;
        double lng = coordinate.lng;

        // İkinci koordinatın daire sınırları içinde mi kontrol et
        return (lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng);
    }

    private static LatLng[] calculateBounds(LatLng center, double radius) {
        // Dairenin sınırlarını hesapla (yukarıda açıklanan yöntem)
        double lat = center.lat;
        double lng = center.lng;

        // Yarıçap için enlem ve boylam sınırlarını hesapla
        double latDelta = radius / 111319.9;
        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;

        double lngDelta = radius / (111319.9 * Math.cos(Math.toRadians(lat)));
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        return new LatLng[] { new LatLng(minLat, minLng), new LatLng(maxLat, maxLng) };
    }

    public static void main(String[] args) {
        // İlk koordinatı ve yarıçapı belirle
        LatLng center = new LatLng(40.7128, -74.0060); // İlk koordinat
        double radius = 100; // İlk yarıçap

        // İkinci koordinatı belirle
        LatLng secondCoordinate = new LatLng(40.7129, -74.0059); // İkinci koordinat
        double secondRadius = 150; // İkinci yarıçap

        // İkinci koordinatın ilk koordinatın içinde olup olmadığını kontrol et
        boolean isInside = isCoordinateInsideCircle(secondCoordinate, center, radius);

        // Sonucu yazdır
        if (isInside) {
            System.out.println("Second coordinate is inside the circle.");
        } else {
            System.out.println("Second coordinate is outside the circle.");
        }
    }
}

