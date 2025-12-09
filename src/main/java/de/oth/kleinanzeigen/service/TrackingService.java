package de.oth.kleinanzeigen.service;

import de.oth.kleinanzeigen.dto.TopSearchTerm;
import de.oth.kleinanzeigen.model.SearchLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class TrackingService {

    private final MongoTemplate mongoTemplate;

    public TrackingService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void logSearch(String query, GeoJsonPoint location, Integer radius, int resultCount, String ipAddress) {
        SearchLog log = new SearchLog(Instant.now(), query, location, radius, resultCount, ipAddress);
        mongoTemplate.save(log);
    }

    public List<TopSearchTerm> getTopSearchTerms(int limit) {
        Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);

        Aggregation aggregation = newAggregation(
                match(Criteria.where("timestamp").gte(last24Hours).and("query").ne(null).ne("")),
                group("query").count().as("count"), sort(Sort.Direction.DESC, "count"), limit(limit),
                project("count").and("_id").as("term"));

        AggregationResults<TopSearchTerm> results = mongoTemplate.aggregate(aggregation, "search_logs",
                TopSearchTerm.class);
        return results.getMappedResults();
    }
}
