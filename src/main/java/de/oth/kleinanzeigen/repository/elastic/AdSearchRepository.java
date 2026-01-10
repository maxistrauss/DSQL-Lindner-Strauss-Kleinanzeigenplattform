package de.oth.kleinanzeigen.repository.elastic;

import de.oth.kleinanzeigen.model.elastic.AdSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdSearchRepository extends ElasticsearchRepository<AdSearchDocument, String> {
}
