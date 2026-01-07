package de.oth.kleinanzeigen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "de.oth.kleinanzeigen.repository.mongo")
@EnableElasticsearchRepositories(basePackages = "de.oth.kleinanzeigen.repository.elastic")
public class KleinanzeigenApplication {

    public static void main(String[] args) {
        SpringApplication.run(KleinanzeigenApplication.class, args);
    }

}