package de.oth.kleinanzeigen.repository;

import de.oth.kleinanzeigen.model.Ad;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdRepository extends MongoRepository<Ad, String> {
}
