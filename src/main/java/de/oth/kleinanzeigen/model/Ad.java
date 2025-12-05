package de.oth.kleinanzeigen.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "ads")
public class Ad {

    @Id
    private String id;
    private String title;
    private String description;
    private String category;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private Instant publishedAt;

    private Seller seller;
    private List<Comment> comments;
    private String imageId;
    private String address;
    private GeoJsonPoint location;

    @ReadOnlyProperty
    private int numberOfComments;

    private Double distanceKm;
    @TextScore
    private Double score;

    public Ad() {
    }

    public Ad(String title, String description, String category, BigDecimal price, Instant publishedAt, Seller seller,
            List<Comment> comments, String imageId, String address, GeoJsonPoint location) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.publishedAt = publishedAt;
        this.seller = seller;
        this.comments = comments;
        this.imageId = imageId;
        this.address = address;
        this.location = location;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public GeoJsonPoint getLocation() { return location; }
    public void setLocation(GeoJsonPoint location) { this.location = location; }

    public int getNumberOfComments() {
        return numberOfComments;
    }
    public void setNumberOfComments(int numberOfComments) {
        this.numberOfComments = numberOfComments;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Double getScore() {
        return score;
    }
    public void setScore(Double score) { this.score = score; }
}
