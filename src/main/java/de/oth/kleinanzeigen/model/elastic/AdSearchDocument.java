package de.oth.kleinanzeigen.model.elastic;

import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.Seller;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document(indexName = "ads")
public class AdSearchDocument {

    @Id
    private String id;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "german"),
        otherFields = {
                @InnerField(suffix = "raw", type = FieldType.Keyword)
        }
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "german")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant publishedAt;

    @Field(type = FieldType.Keyword)
    private String imageId;

    @Field(type = FieldType.Text)
    private String address;

    @GeoPointField
    private GeoPoint location;

    // statistical fields
    private int numberOfComments;
    @Transient
    private Float score;
    @ScriptedField
    private Double distanceKm;

    public AdSearchDocument() {}
    public AdSearchDocument(
            String title, String description, String category,
            BigDecimal price, Instant publishedAt,String imageId,
            String address, GeoPoint location) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.publishedAt = publishedAt;
        this.imageId = imageId;
        this.address = address;
        this.location = location;
    }
}
