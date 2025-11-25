package de.oth.kleinanzeigen.service;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GeoServiceOSM {

    private final RestTemplate rest = new RestTemplate();

    public GeoJsonPoint geocode(String address) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
                    + "&format=json&limit=1";

            GeoResult[] results = rest.getForObject(url, GeoResult[].class);

            if(results != null && results.length > 0) {
                double lat = Double.parseDouble(results[0].lat);
                double lng = Double.parseDouble(results[0].lon);
                return new GeoJsonPoint(lng, lat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static class GeoResult {
        public String lat;
        public String lon;
    }
    public String reverseGeocode(double lat, double lon) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?lat="
                    + URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8)
                    + "&lon=" + URLEncoder.encode(String.valueOf(lon), StandardCharsets.UTF_8)
                    + "&format=json";

            ReverseResult result = rest.getForObject(url, ReverseResult.class);

            if(result != null && result.address != null) {
                // z.B. Straße, PLZ, Ort zusammenbauen
                String addr = "";
                if(result.address.road != null) addr += result.address.road + ", ";
                if(result.address.postcode != null) addr += result.address.postcode + " ";
                if(result.address.city != null) addr += result.address.city;
                return addr.trim();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static class ReverseResult {
        public Address address;

        static class Address {
            public String road;
            public String postcode;
            public String city;
        }
    }
}
