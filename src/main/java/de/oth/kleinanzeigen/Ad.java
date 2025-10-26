package de.oth.kleinanzeigen;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ads")
public class Ad {

    @Id
    private String id;
    private String title;
    private String description;
    // TODO only for testing, remove later
    private String imageUrl;
    private double price;

    public Ad() {
    }

    public Ad(String title, String description, double price) {
        this.title = title;
        this.description = description;
        this.price = price;
    }

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

    public String getImageUrl() { return imageUrl; }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getPrice() { return price; }

    public void setPrice(double price) { this.price = price; }

    @Override
    public String toString() {
        return "Ad{" +
               "id='" + id + "'" +
               ", title='" + title + "'" +
               ", description='" + description + "'" +
               '}';
    }
}
