package de.oth.kleinanzeigen;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final AdRepository adRepository;

    public DatabaseInitializer(AdRepository adRepository) {
        this.adRepository = adRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Initializing Database with sample data ---");

        // Clear existing data for a clean start (optional)
        adRepository.deleteAll();

        // Save a sample ad
        Ad sampleAd = new Ad("Test Ad Title", "This is a test description for a sample ad.", 0.0);
        adRepository.save(sampleAd);
        System.out.println("Saved sample ad: " + sampleAd);

        // Retrieve all ads and print them
        List<Ad> ads = adRepository.findAll();
        System.out.println("--- All Ads in Database ---");
        ads.forEach(System.out::println);
        System.out.println("--- Database Initialization Complete ---");
    }
}
