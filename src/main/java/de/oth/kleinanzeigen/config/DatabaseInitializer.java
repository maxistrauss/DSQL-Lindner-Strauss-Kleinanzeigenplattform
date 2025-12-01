package de.oth.kleinanzeigen.config;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.Seller;
import de.oth.kleinanzeigen.repository.AdRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
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

    private final Map<String, String> categoryImageCache = new HashMap<>();

    public DatabaseInitializer(AdRepository adRepository, GridFsTemplate gridFsTemplate) {
        this.adRepository = adRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if database is already populated
        if (adRepository.count() > 0) {
            System.out.println("--- Database already initialized. Skipping sample data creation. ---");
            return;
        }

        System.out.println("--- Initializing Database with sample data ---");

        // Save a sample ad (fixed data)
        createSampleAd();

        // Generate random ads
        generateRandomAds(20);

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
