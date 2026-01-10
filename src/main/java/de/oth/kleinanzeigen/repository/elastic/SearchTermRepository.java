package de.oth.kleinanzeigen.repository.elastic;

import de.oth.kleinanzeigen.model.elastic.SearchTerm;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchTermRepository extends ElasticsearchRepository<SearchTerm, String> {
}
