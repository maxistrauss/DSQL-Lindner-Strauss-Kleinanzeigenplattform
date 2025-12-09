package de.oth.kleinanzeigen.config;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.SearchLog;
import de.oth.kleinanzeigen.model.Seller;
import de.oth.kleinanzeigen.repository.AdRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final AdRepository adRepository;
    private final GridFsTemplate gridFsTemplate;
    private final MongoTemplate mongoTemplate;

    private final Map<String, String> categoryImageCache = new HashMap<>();

    public DatabaseInitializer(AdRepository adRepository, GridFsTemplate gridFsTemplate, MongoTemplate mongoTemplate) {
        this.adRepository = adRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if database already contains data
        long adCount = mongoTemplate.count(new BasicQuery("{}"), Ad.class, "ads");
        if (adCount > 0) {
            System.out.println("--- Database already contains data. Skipping initialization. ---");
            return;
        }

        // Create Indexes manually
        try {
            IndexOperations indexOps = mongoTemplate.indexOps(Ad.class);

            // Text Index
            indexOps.createIndex(new TextIndexDefinition.TextIndexDefinitionBuilder().onField("title").withDefaultLanguage("german").build());
            // Geo Index
            indexOps.createIndex(new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
            // Compound Index: Category + Date
            indexOps.createIndex(new Index().on("category", Sort.Direction.ASC).on("publishedAt", Sort.Direction.DESC));
            // Single Field Indexes
            indexOps.createIndex(new Index().on("price", Sort.Direction.ASC));
            indexOps.createIndex(new Index().on("publishedAt", Sort.Direction.DESC));
            indexOps.createIndex(new Index().on("seller.username", Sort.Direction.ASC));

            System.out.println("--- Indexes for 'Ad' created manually ---");
        } catch (Exception e) {
            System.err.println("Error creating indexes: " + e.getMessage());
        }

        // Initialize 'search_logs' Capped Collection
        if (!mongoTemplate.collectionExists("search_logs")) {
            CollectionOptions options
                = CollectionOptions
                    .empty()
                    .capped()
                    .size(5242880) // 5 MB
                    .maxDocuments(5000);
            mongoTemplate.createCollection("search_logs", options);
            GeospatialIndex locationIndex = new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE);
            mongoTemplate.indexOps(SearchLog.class).createIndex(locationIndex);
            System.out.println("--- Capped Collection 'search_logs' created ---");
        }

        // Initialize 'ads_view' View
        try {
            com.mongodb.client.MongoDatabase db = mongoTemplate.getDb();
            boolean exists = false;
            for (org.bson.Document doc : db.listCollections()) {
                if ("ads_view".equals(doc.getString("name"))) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                db.getCollection("ads_view").drop();
            }

            String viewName = "ads_view";
            String viewOn = "ads";
            List<org.bson.Document> pipeline = List.of(new org.bson.Document("$addFields",
                    new org.bson.Document("numberOfComments", new org.bson.Document("$size",
                            new org.bson.Document("$ifNull", List.of("$comments", List.of()))))));
            db.createView(viewName, viewOn, pipeline);
            System.out.println("--- View 'ads_view' created ---");

        } catch (Exception e) {
            System.err.println("Error creating view 'ads_view': " + e.getMessage());
        }

        System.out.println("--- Initializing Database with sample data ---");

        // Save a sample ad (fixed data)
        createSampleAd();

        // Generate random ads
        generateRandomAds(42);

        // Retrieve all ads and print them
        List<Ad> ads = adRepository.findAll();
        System.out.println("--- All Ads in Database ---");
        ads.forEach(System.out::println);
        System.out.println("--- Database Initialization Complete ---");
    }

    private void createSampleAd() {
        Seller seller = new Seller();
        seller.setUsername("Max_Mustermann");
        seller.setPhoneNumber("0123456789");

        Comment comment = new Comment();
        comment.setUserId("user123");
        comment.setText("Super Angebot!");
        comment.setCreatedAt(LocalDateTime.now().minusDays(1));

        Ad sampleAd = new Ad("Gebrauchtes Fahrrad", "Gut erhaltenes Herrenrad, 28 Zoll, 7 Gänge. Kaum gefahren.",
                "Fahrräder", new BigDecimal("150.00"), Instant.now().minusSeconds(60 * 60 * 24 * 3), seller,
                List.of(comment), null, "Hauptstraße 22, 85354 Freising", new GeoJsonPoint(2, 3));

        // Try to get an image for the sample ad
        String sampleImageId = getOrFetchImageId("Fahrräder");
        sampleAd.setImageId(sampleImageId);

        adRepository.save(sampleAd);
        System.out.println("Saved sample ad: " + sampleAd);
    }

    private void generateRandomAds(int count) {
        List<String> categories = List.of("Elektronik", "Fahrräder", "Bücher", "Kleidung", "Möbel", "Haushalt",
                "Multimedia");
        List<String> adjectives = List.of("Neu", "Gebraucht", "Alt", "Defekt", "Wie neu", "Selten", "Schön", "Retro",
                "Modern", "Hochwertig");
        List<String> nouns = List.of("iPhone", "Fahrrad", "Sofa", "Tisch", "Laptop", "Monitor", "Buch", "Hose", "Jacke",
                "Lampe", "Kamera", "Uhr", "Stuhl", "Schrank");
        List<String> sellerNames = List.of("Max", "Anna", "Tom", "Lisa", "John", "Sarah", "Tim", "Julia", "Michael",
                "Sandra", "Peter", "Marie");

        // Sample locations for GeoData
        record LocationData(String address, double lat, double lon) {
        }
        List<LocationData> locations = List.of(
                new LocationData("Galgenbergstr. 30, 93053 Regensburg", 49.0068, 12.0963),
                new LocationData("Marienplatz 1, 80331 München", 48.1374, 11.5755),
                new LocationData("Alexanderplatz 1, 10178 Berlin", 52.5219, 13.4132),
                new LocationData("Domkloster 4, 50667 Köln", 50.9413, 6.9583),
                new LocationData("Neuer Jungfernstieg 20, 20354 Hamburg", 53.5551, 9.9937),
                new LocationData("Schlossplatz 1, 70173 Stuttgart", 48.7784, 9.1800),
                new LocationData("Prinzipalmarkt 10, 48143 Münster", 51.9625, 7.6256),
                new LocationData("Markt 1, 04109 Leipzig", 51.3404, 12.3748),
                new LocationData("Königstraße 1, 01097 Dresden", 51.0600, 13.7400),
                new LocationData("Zeil 100, 60313 Frankfurt am Main", 50.1144, 8.6821),
                new LocationData("Universitätsstraße 31, 93053 Regensburg", 49.0018, 12.0967),
                new LocationData("Domplatz 1, 99084 Erfurt", 50.9761, 11.0235),
                new LocationData("Schlossallee 1, 19053 Schwerin", 53.6231, 11.4141),
                new LocationData("Rathausplatz 1, 86150 Augsburg", 48.3689, 10.8981),
                new LocationData("Willy-Brandt-Platz 1, 45127 Essen", 51.4523, 7.0112),
                new LocationData("Kröpcke, 30159 Hannover", 52.3745, 9.7386),
                new LocationData("Alter Markt 1, 24103 Kiel", 54.3233, 10.1400),
                new LocationData("Breite Straße 1, 14467 Potsdam", 52.3980, 13.0530),
                new LocationData("Marktplatz 1, 76133 Karlsruhe", 49.0092, 8.4040),
                new LocationData("Am Markt 1, 28195 Bremen", 53.0760, 8.8080));

        Random random = new Random();

        for (int i = 0; i < count; i++) {
            String noun = nouns.get(random.nextInt(nouns.size()));
            String adjective = adjectives.get(random.nextInt(adjectives.size()));
            String title = adjective + "es " + noun;

            String description = "Verkaufe hier mein " + noun + ". Es ist " + adjective.toLowerCase()
                    + " und in gutem Zustand. Bei Fragen gerne melden.";
            String category = categories.get(random.nextInt(categories.size()));
            BigDecimal price = new BigDecimal(random.nextInt(500) + 10);

            Seller seller = new Seller();
            seller.setUsername(sellerNames.get(random.nextInt(sellerNames.size())) + random.nextInt(100));
            seller.setPhoneNumber("0151" + (10000000 + random.nextInt(90000000)));

            LocationData loc = locations.get(random.nextInt(locations.size()));
            GeoJsonPoint point = new GeoJsonPoint(loc.lon(), loc.lat());

            // Generate random comments
            List<Comment> comments = generateRandomComments(random);

            // Fetch or get cached image
            String imageId = getOrFetchImageId(category);

            Ad ad = new Ad(title, description, category, price,
                    Instant.now().minusSeconds(random.nextInt(3600 * 24 * 30)), // random time in last 30 days
                    seller, comments, imageId, loc.address(), point);
            adRepository.save(ad);
        }
        System.out.println("Generated " + count + " random ads.");
    }

    private List<Comment> generateRandomComments(Random random) {
        List<Comment> comments = new ArrayList<>();
        // 50% chance of having comments
        if (random.nextBoolean()) {
            int numComments = random.nextInt(4) + 1; // 1 to 4 comments
            List<String> commentTexts = List.of("Noch zu haben?", "Was ist letzte Preis?", "Top Zustand!",
                    "Schneller Versand, gerne wieder.", "Kann man am Preis noch was machen?", "Interesse!",
                    "Sieht gut aus.", "Funktioniert alles einwandfrei?", "Toller Verkäufer!",
                    "Würde es sofort nehmen.");
            List<String> users = List.of("Buyer1", "Interessent2", "SchnäppchenJäger", "User123", "Gast", "Lukas",
                    "Sophie");

            for (int i = 0; i < numComments; i++) {
                Comment c = new Comment();
                c.setText(commentTexts.get(random.nextInt(commentTexts.size())));
                c.setUserId(users.get(random.nextInt(users.size())));
                c.setCreatedAt(LocalDateTime.now().minusHours(random.nextInt(200)));
                comments.add(c);
            }
        }
        return comments;
    }

    private String getOrFetchImageId(String category) {
        // Return cached ID if available
        if (categoryImageCache.containsKey(category)) {
            return categoryImageCache.get(category);
        }

        String imageUrl = getImageUrlForCategory(category);
        if (imageUrl == null)
            return null;

        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            // Store image in GridFS
            String imageId = gridFsTemplate.store(inputStream, category + ".jpg", "image/jpeg").toString();
            categoryImageCache.put(category, imageId);
            System.out.println("Downloaded and stored image for category " + category + ": " + imageId);
            return imageId;
        } catch (Exception e) {
            System.err.println("Failed to download image for category " + category + ": " + e.getMessage());
            // Return null or maybe a default placeholder if download fails
            return null;
        }
    }

    private String getImageUrlForCategory(String category) {
        // Using loremflickr for placeholder images with category keywords
        String baseUrl = "https://loremflickr.com/640/480/";
        return switch (category) {
            case "Elektronik" -> baseUrl + "electronics";
            case "Fahrräder" -> baseUrl + "bicycle";
            case "Bücher" -> baseUrl + "book";
            case "Kleidung" -> baseUrl + "clothing";
            case "Möbel" -> baseUrl + "furniture";
            case "Haushalt" -> baseUrl + "household";
            case "Multimedia" -> baseUrl + "multimedia";
            default -> baseUrl + "abstract";
        };
    }
}
