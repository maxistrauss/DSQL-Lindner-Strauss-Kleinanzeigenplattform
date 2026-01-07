package de.oth.kleinanzeigen.model.elastic;

import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.Seller;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(indexName = "ads")
public class Ad {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "german")
    private String title;

    @Field(type = FieldType.Text, analyzer = "german")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant publishedAt;

    @Field(type = FieldType.Object)
    private Seller seller;

    @Field(type = FieldType.Nested)
    private List<Comment> comments;

    @Field(type = FieldType.Keyword)
    private String imageId;

    @Field(type = FieldType.Text)
    private String address;

    @GeoPointField
    private GeoPoint location;

    @ReadOnlyProperty
    private int numberOfComments;

    @WriteOnlyProperty
    private Double distanceKm;
    @WriteOnlyProperty
    private Double score;

    public Ad() {
    }

    public Ad(String title, String description, String category, BigDecimal price, Instant publishedAt, Seller seller,
            List<Comment> comments, String imageId, String address, GeoPoint location) {
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

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

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
