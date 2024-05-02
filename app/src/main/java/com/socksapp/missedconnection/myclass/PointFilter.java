package com.socksapp.missedconnection.myclass;

import com.google.maps.model.LatLng;

public class PointFilter {

    public static boolean isPointInsideCircle(LatLng point, LatLng center, double radius) {

        LatLng[] bounds = calculateBounds(center, radius);
        double minLat = bounds[0].lat;
        double minLng = bounds[0].lng;
        double maxLat = bounds[1].lat;
        double maxLng = bounds[1].lng;

        // Noktanın koordinatlarını al
        double lat = point.lat;
        double lng = point.lng;

        // Nokta daire sınırları içinde mi kontrol et
        return (lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng);
    }

    private static LatLng[] calculateBounds(LatLng center, double radius) {
        // Dairenin sınırlarını hesapla (yukarıda açıklanan yöntem)
        double lat = center.lat;
        double lng = center.lng;

        // 100 metrelik yarıçap için enlem ve boylam sınırlarını hesapla
        double latDelta = radius / 111319.9;
        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;

        // Doğu ve batı yönünde boylam sınırlarını hesapla
        double lngDelta = radius / (111319.9 * Math.cos(Math.toRadians(lat)));
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        return new LatLng[] { new LatLng(minLat, minLng), new LatLng(maxLat, maxLng) };
    }

    public static void main(String[] args) {
        // Merkez noktası ve yarıçapı belirle
        LatLng center = new LatLng(40.7128, -74.0060); // Örnek New York'un koordinatları
        double radius = 100; // 100 metrelik yarıçap

        // Nokta belirle (örneğin, veritabanından alınan bir nokta)
        LatLng point = new LatLng(40.7129, -74.0059); // Örnek bir nokta

        // Noktanın daire içinde olup olmadığını kontrol et
        boolean isInsideCircle = isPointInsideCircle(point, center, radius);

        // Sonucu yazdır
        if (isInsideCircle) {
            System.out.println("Point is inside the circle.");
        } else {
            System.out.println("Point is outside the circle.");
        }
    }
}

