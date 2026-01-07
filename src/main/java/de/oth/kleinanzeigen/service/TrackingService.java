package de.oth.kleinanzeigen.service;

import de.oth.kleinanzeigen.dto.TopSearchTerm;
import de.oth.kleinanzeigen.model.SearchLog;
import de.oth.kleinanzeigen.model.elastic.SearchTerm;
import de.oth.kleinanzeigen.repository.elastic.SearchTermRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TrackingService {

    private final MongoTemplate mongoTemplate;
    private final SearchTermRepository searchTermRepository;

    public TrackingService(MongoTemplate mongoTemplate, SearchTermRepository searchTermRepository) {
        this.mongoTemplate = mongoTemplate;
        this.searchTermRepository = searchTermRepository;
    }

    public void logSearch(String query, GeoJsonPoint location, Integer radius, int resultCount, String ipAddress) {
        SearchLog log = new SearchLog(Instant.now(), query, location, radius, resultCount, ipAddress);
        mongoTemplate.save(log);

        if (query != null && !query.isBlank() && resultCount > 0) {
            updateElasticSearchTerm(query.trim());
        }
    }

    private void updateElasticSearchTerm(String query) {
        String id = query.toLowerCase();
        Optional<SearchTerm> existing = searchTermRepository.findById(id);
        if (existing.isPresent()) {
            SearchTerm st = existing.get();
            st.setCount(st.getCount() + 1);
            searchTermRepository.save(st);
        } else {
            searchTermRepository.save(new SearchTerm(query, 1));
        }
    }

    public List<TopSearchTerm> getTopSearchTerms(int limit) {
        // Fetch from Elasticsearch instead of MongoDB aggregation
        return searchTermRepository.findAll(org.springframework.data.domain.PageRequest.of(0, limit,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "count")))
                .stream().map(st -> new TopSearchTerm(st.getTerm(), (long) st.getCount()))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<TopSearchTerm> getTopSearchTermsFiltered(String term, int limit) {
        // This is now redundant since KleinanzeigenService.suggestTrendingTerms does it in ES
        // but for compatibility we can implement it here or call the service
        return getTopSearchTerms(limit).stream()
                .filter(t -> term == null || t.getTerm().toLowerCase().contains(term.toLowerCase())).limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }
}
