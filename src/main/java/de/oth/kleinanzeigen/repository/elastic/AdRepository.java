package de.oth.kleinanzeigen.repository.elastic;

import de.oth.kleinanzeigen.model.elastic.Ad;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AdRepository extends ElasticsearchRepository<Ad, String> {
}
