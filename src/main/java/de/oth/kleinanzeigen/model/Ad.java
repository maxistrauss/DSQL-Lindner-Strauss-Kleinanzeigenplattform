package de.oth.kleinanzeigen.model;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
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
}
