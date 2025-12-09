package de.oth.kleinanzeigen.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "search_logs")
public class SearchLog {
    @Id
    private String id;
    private Instant timestamp;
    private String query;
    private GeoJsonPoint location;
    private Integer radius; // in km
    private int resultCount;
    private String ipAddress;

    public SearchLog() {
    }

    public SearchLog(Instant timestamp, String query, GeoJsonPoint location, Integer radius, int resultCount,
            String ipAddress) {
        this.timestamp = timestamp;
        this.query = query;
        this.location = location;
        this.radius = radius;
        this.resultCount = resultCount;
        this.ipAddress = ipAddress;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public GeoJsonPoint getLocation() {
        return location;
    }

    public void setLocation(GeoJsonPoint location) {
        this.location = location;
    }

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
