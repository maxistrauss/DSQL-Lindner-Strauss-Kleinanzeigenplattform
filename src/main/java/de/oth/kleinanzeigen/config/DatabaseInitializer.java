package de.oth.kleinanzeigen.config;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.Seller;
import de.oth.kleinanzeigen.repository.AdRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final AdRepository adRepository;

    public DatabaseInitializer(AdRepository adRepository) {
        this.adRepository = adRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (adRepository.count() > 0) {
            System.out.println("--- Database already initialized. Skipping sample data creation. ---");
            return;
        }

        System.out.println("--- Initializing Database with sample data ---");

        // Save a sample ad
        Seller seller = new Seller();
        seller.setUsername("Max_Mustermann");
        seller.setPhoneNumber("0123456789");

        Comment comment = new Comment();
        comment.setUserId("user123");
        comment.setText("Super Angebot!");
        comment.setRating(5);
        comment.setCreatedAt(LocalDateTime.now().minusDays(1));

        Ad sampleAd = new Ad("Gebrauchtes Fahrrad", "Gut erhaltenes Herrenrad, 28 Zoll, 7 Gänge. Kaum gefahren.",
                "Fahrräder", new BigDecimal("150.00"), Instant.now().minusSeconds(60 * 60 * 24 * 3), seller,
                List.of(comment), null);

        adRepository.save(sampleAd);
        System.out.println("Saved sample ad: " + sampleAd);

        // Retrieve all ads and print them
        List<Ad> ads = adRepository.findAll();
        System.out.println("--- All Ads in Database ---");
        ads.forEach(System.out::println);
        System.out.println("--- Database Initialization Complete ---");
    }
}
