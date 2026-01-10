package de.oth.kleinanzeigen.repository.mongo;

import de.oth.kleinanzeigen.model.Ad;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdRepository extends MongoRepository<Ad, String> {
}
