package de.oth.kleinanzeigen.repository.elastic;

import de.oth.kleinanzeigen.model.elastic.SearchTerm;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SearchTermRepository extends ElasticsearchRepository<SearchTerm, String> {
}
