package de.oth.kleinanzeigen.service;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.AdView;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class KleinanzeigenService {

    // TODO MongoTemplate or MongoRepository ??
    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Ad> findRandomAds(int n) {
        BasicQuery query = new BasicQuery("{}", "{}");
        List<AdView> ads = mongoTemplate.find(query, AdView.class, "ads_view");

        // swap n randomly picked elements to the end of the list
        List<Ad> copy = new ArrayList<>(ads);
        Collections.shuffle(copy);

        // take the last n elements
        return copy.subList(0, Math.min(n, copy.size()));
    }

    public Page<Ad> findAdsByCategory(String category, String sortOption, int page, int pageSize) {
        return findAds(null, category, null, 0, sortOption, page, pageSize);
    }

    public Page<Ad> findAds(
            String searchTitle, String category,
            GeoJsonPoint location, int radius,
            String sortOption,
            int page, int pageSize) {

        long totalElements = 0L;

        boolean hasText = searchTitle != null && !searchTitle.isBlank();
        boolean hasGeo  = location != null;


        // Pagination
        Pageable  pageable = PageRequest.of(Math.max(0, page), pageSize);
        long skip = pageable.getOffset();
        long limit = pageable.getPageSize();


        Query baseQuery = new Query();
        List<Criteria> filters = new ArrayList<>();
        List<AggregationOperation> aggPipeline = new ArrayList<>();

        // criteria
        if (category != null && !category.isBlank()) {
            Pattern catPattern = Pattern.compile("^" + Pattern.quote(category) + "$", Pattern.CASE_INSENSITIVE);
            filters.add(Criteria.where("category").regex(catPattern));
        }

        // combine criteria in base query
        for(CriteriaDefinition criteria : filters) {
            baseQuery.addCriteria(criteria);
        }

        if (hasGeo) {
            if(hasText) {
                baseQuery.addCriteria(Criteria.where("title").regex(searchTitle));
            }

            NearQuery nearQuery
                = NearQuery.near(location, Metrics.KILOMETERS)
                    .maxDistance(radius, Metrics.KILOMETERS)
                    .spherical(true)
                    .query(baseQuery);

            aggPipeline.add(Aggregation.geoNear(nearQuery, "distanceKm"));
        } else if(hasText) { // if text search without geo --> use TextQuery to get text score for relevance sorting
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(searchTitle);
            TextQuery query = TextQuery.queryText(textCriteria);

            aggPipeline.add(context -> new Document("$match", query.getQueryObject()));
            aggPipeline.add(Aggregation.addFields().addFieldWithValue("score", new Document("$meta", "textScore")).build());
        }
        else {
            Query query = Query.of(baseQuery);
            aggPipeline.add(context -> new Document("$match", query.getQueryObject()));
        }

        // get total elements (needed for pagination) - sort, skip, limit irrelevant here
        List<AggregationOperation> countPipeline = new ArrayList<>(aggPipeline);
        countPipeline.add(Aggregation.count().as("totalResults"));
        Aggregation countAgg = Aggregation.newAggregation(countPipeline);
        AggregationResults<Document> countAggResult = mongoTemplate.aggregate(countAgg, "ads", Document.class);
        if (!countAggResult.getMappedResults().isEmpty()) {
            totalElements = countAggResult.getMappedResults().getFirst().getInteger("totalResults", 0);
        }

        // determine and add sort stage based on option
        AggregationOperation sortOp = buildSortOp(sortOption, hasText, hasGeo);
        aggPipeline.add(sortOp);

        // add pagination
        aggPipeline.add(Aggregation.skip(skip));
        aggPipeline.add(Aggregation.limit(limit));

        Aggregation dataAgg = Aggregation.newAggregation(aggPipeline);
        AggregationResults<Ad> aggResults = mongoTemplate.aggregate(dataAgg, "ads",  Ad.class);
        List<Ad> results = new ArrayList<>(aggResults.getMappedResults());

        // Manually calculate numberOfComments since we are not using the view
        results.forEach(ad -> ad.setNumberOfComments(ad.getComments() != null ? ad.getComments().size() : 0));

        return new PageImpl<>(results, pageable, totalElements);
    }

    public List<String> findAllCategories() {
        BasicQuery query = new BasicQuery("{}", "{category: 1, _id:  0}");
        query.with(Sort.by(Sort.Direction.ASC, "category"));
        return mongoTemplate.findDistinct(query, "category", Ad.class, String.class);
    }

    public boolean hasCategoryAds(String category) {
        BasicQuery query = new BasicQuery(String.format("{category: '%s'}", category), "{}");
        return !mongoTemplate.find(query, Ad.class).isEmpty();
    }

    public long countAllAds() {
        return mongoTemplate.count(new BasicQuery("{}"), Ad.class);
    }

    private AggregationOperation buildSortOp(String sortOption, boolean hasText, boolean hasGeo) {
        return switch (sortOption == null ? "" : sortOption) {
            // relevance by text score only possible if hasText and no geo
            case "relevance" -> {
                if(hasText && !hasGeo)
                    yield Aggregation.sort(Sort.by("score").descending().and(Sort.by("publishedAt").descending()));
                else if(hasGeo)
                    yield Aggregation.sort(Sort.by("distanceKm").ascending().and(Sort.by("publishedAt").descending()));
                else
                    yield Aggregation.sort(Sort.by("publishedAt").descending());
            }
            case "title_asc" -> Aggregation.sort(Sort.by("title").ascending());
            case "title_desc" -> Aggregation.sort(Sort.by("title").descending());
            case "price_asc" -> Aggregation.sort(Sort.by("price").ascending().and(Sort.by("publishedAt").descending()));
            case "price_desc" -> Aggregation.sort(Sort.by("price").descending().and(Sort.by("publishedAt").descending()));
            case "distance_asc" -> Aggregation.sort(Sort.by("distanceKm").ascending());
            case "distance_desc" -> Aggregation.sort(Sort.by("distanceKm").descending());
            default -> Aggregation.sort(Sort.by("publishedAt").descending());
        };
    }
}
