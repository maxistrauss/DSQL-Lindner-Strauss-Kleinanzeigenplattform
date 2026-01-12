package de.oth.kleinanzeigen.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import de.oth.kleinanzeigen.model.elastic.AdSearchDocument;
import de.oth.kleinanzeigen.model.elastic.SearchTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.ScriptData;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.stereotype.Service;

import java.sql.Wrapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KleinanzeigenService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public List<AdSearchDocument> findRandomAds(int n) {
        var funcScoreQuery = QueryBuilders.wrapper().query("{ \"function_score\": { \"query\": { \"match_all\": {} }, \"random_score\": {} } }").build();

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(Query.of(
                        q -> q.functionScore(
                                f -> f
                                        .query(
                                        m -> m.matchAll(x -> x))
                                        .functions(r -> r.randomScore(a -> a)))))
                .withPageable(PageRequest.of(0, n))
                .build();
        return elasticsearchOperations.search(query, AdSearchDocument.class).stream().map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public Page<AdSearchDocument> findAdsByCategory(String category, String sortOption, int page, int pageSize) {
        return findAds(null, category, null, 0, sortOption, page, pageSize);
    }

    public Page<AdSearchDocument> findAds(
            String searchTitle, String category, GeoPoint location, int radius,
            String sortOption,
            int page, int pageSize) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (category != null && !category.isBlank()) {
            boolQueryBuilder.filter(Query.of(q -> q.term(t -> t.field("category").value(category))));
        }

        if (searchTitle != null && !searchTitle.isBlank()) {
            boolQueryBuilder.must(Query.of(q -> q.bool(b -> b.should(
                            s -> s.multiMatch(m -> m.fields("title^3", "description").query(searchTitle).fuzziness("AUTO")))
                    .should(s -> s.multiMatch(m -> m.fields("title^5", "description^2").query(searchTitle)
                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.Phrase)))
                    .minimumShouldMatch("1"))));
        }

        if (location != null) {

            boolQueryBuilder.filter(Query.of(q -> q.geoDistance(g -> g

                    .field("location")

                    .distance(radius + "km")

                    .location(l -> l.latlon(ll -> ll.lat(location.getLat()).lon(location.getLon())))

            )));

        }

        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()

                .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))

                .withPageable(PageRequest.of(Math.max(0, page), pageSize))

                .withTrackScores(true); // Ensure scores are calculated even when sorting

        if(location != null) {
            String src = "doc['location'].arcDistance(params.lat, params.lon)/1000";
            Map<String, Object> params = Map.of("lat", location.getLat(), "lon", location.getLon());

            ScriptData scriptData = ScriptData.builder()
                    .withScript(src)
                    .withParams(params)
                    .withType(ScriptType.INLINE)
                    .build();

            ScriptedField scriptedField = new ScriptedField("distanceKm", scriptData);
            queryBuilder.withScriptedField(scriptedField);
        }
        queryBuilder.withSourceFilter(FetchSourceFilter.of(b -> b.withIncludes("*")));
        boolean hasText = searchTitle != null && !searchTitle.isBlank();
        applySorting(queryBuilder, sortOption, hasText, location);

        SearchHits<AdSearchDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), AdSearchDocument.class);

        List<AdSearchDocument> ads = searchHits.getSearchHits().stream()

                .map(hit -> {

                    AdSearchDocument ad = hit.getContent();
                    ad.setScore(Float.isNaN(hit.getScore()) ? 0.0f : hit.getScore());

                    return ad;

                })

                .collect(Collectors.toList());

        

        return new PageImpl<>(ads, PageRequest.of(Math.max(0, page), pageSize), searchHits.getTotalHits());
    }

    private void applySorting(NativeQueryBuilder queryBuilder, String sortOption, boolean hasText, GeoPoint location) {
        switch (sortOption == null ? "" : sortOption) {
            case "relevance":
                    queryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
                break;
            case "distance":
                queryBuilder.withSort(s -> s.geoDistance(g -> g.field("location")
                        .location(l -> l.latlon(ll -> ll.lat(location.getLat()).lon(location.getLon())))
                        .order(SortOrder.Asc)));
                break;
            case "title_asc":
                queryBuilder.withSort(s -> s.field(f -> f.field("title.raw").order(SortOrder.Asc)));
                break;
            case "title_desc":
                queryBuilder.withSort(s -> s.field(f -> f.field("title.raw").order(SortOrder.Desc)));
                break;
            case "price_asc":
                queryBuilder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
                break;
            case "price_desc":
                queryBuilder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
                break;
            case "distance_asc":
                if (location != null)
                    queryBuilder.withSort(s -> s.geoDistance(g -> g.field("location")
                            .location(l -> l.latlon(ll -> ll.lat(location.getLat()).lon(location.getLon())))
                            .order(SortOrder.Asc)));
                break;
            default:
                queryBuilder.withSort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)));
        }
    }

    public List<String> autocomplete(String term) {

        // Improved autocomplete with fuzziness and phrase prefix
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(Query.of(q -> q.bool(b -> b
                        .should(s -> s.matchPhrasePrefix(m -> m.field("title").query(term)))
                        .should(s -> s.match(m -> m.field("title").query(term).fuzziness("AUTO")))
                )))
                .withPageable(PageRequest.of(0, 10))
                .build();

        return elasticsearchOperations.search(query, AdSearchDocument.class)

                .stream()

                .map(hit -> hit.getContent().getTitle())

                .distinct()

                .collect(Collectors.toList());

    }

    public List<String> suggestTrendingTerms(String term) {

        NativeQuery query = new NativeQueryBuilder()

                .withQuery(Query.of(q -> q.matchPhrasePrefix(m -> m.field("term").query(term))))

                .withSort(s -> s.field(f -> f.field("count").order(SortOrder.Desc)))

                .withPageable(PageRequest.of(0, 5))

                .build();

        return elasticsearchOperations.search(query, SearchTerm.class)

                .stream()

                .map(hit -> hit.getContent().getTerm())

                .collect(Collectors.toList());

    }

    

    public List<String> findAllCategories() {
        NativeQuery query = new NativeQueryBuilder().withQuery(Query.of(q -> q.matchAll(m -> m)))
                .withPageable(PageRequest.of(0, 1000)).build();
        return elasticsearchOperations.search(query, AdSearchDocument.class).stream().map(hit -> hit.getContent().getCategory())
                .distinct().sorted().collect(Collectors.toList());
    }

    public boolean hasCategoryAds(String category) {
        NativeQuery query = new NativeQueryBuilder().withQuery(
                        Query.of(q -> q.term(t -> t.field("category").value(category)))).withPageable(PageRequest.of(0, 1))
                .build();
        return elasticsearchOperations.count(query, AdSearchDocument.class) > 0;
    }

    public long countAllAds() {
        NativeQuery query = new NativeQueryBuilder().withQuery(Query.of(q -> q.matchAll(m -> m))).build();
        return elasticsearchOperations.count(query, AdSearchDocument.class);
    }
}
