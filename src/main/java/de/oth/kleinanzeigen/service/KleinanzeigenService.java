package de.oth.kleinanzeigen.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import de.oth.kleinanzeigen.model.elastic.Ad;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KleinanzeigenService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public List<Ad> findRandomAds(int n) {
        NativeQuery query = new NativeQueryBuilder().withQuery(Query.of(q -> q.matchAll(m -> m)))
                .withPageable(PageRequest.of(0, n)).build();
        return elasticsearchOperations.search(query, Ad.class).stream().map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public Page<Ad> findAdsByCategory(String category, String sortOption, int page, int pageSize) {
        return findAds(null, category, null, 0, sortOption, page, pageSize);
    }

    public Page<Ad> findAds(
            String searchTitle, String category, GeoPoint location, int radius,
            String sortOption,
            int page, int pageSize) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (category != null && !category.isBlank()) {
            boolQueryBuilder.filter(Query.of(q -> q.term(t -> t.field("category").value(category))));
        }

        if (searchTitle != null && !searchTitle.isBlank()) {
            boolQueryBuilder.must(Query.of(q -> q.match(m -> m.field("title").query(searchTitle))));
        }

        if (location != null) {
            boolQueryBuilder.filter(Query.of(q -> q.geoDistance(g -> g.field("location").distance(radius + "km")
                    .location(l -> l.latlon(ll -> ll.lat(location.getLat()).lon(location.getLon()))))));
        }

        NativeQueryBuilder queryBuilder = new NativeQueryBuilder().withQuery(
                        Query.of(q -> q.bool(boolQueryBuilder.build())))
                .withPageable(PageRequest.of(Math.max(0, page), pageSize));

        applySorting(queryBuilder, sortOption, searchTitle != null && !searchTitle.isBlank(), location);

        SearchHits<Ad> searchHits = elasticsearchOperations.search(queryBuilder.build(), Ad.class);
        List<Ad> ads = searchHits.getSearchHits().stream().map(hit -> {
            Ad ad = hit.getContent();
            ad.setScore(hit.getScore() != Float.NaN ? (double) hit.getScore() : 0.0);
            ad.setNumberOfComments(ad.getComments() != null ? ad.getComments().size() : 0);
            return ad;
        }).collect(Collectors.toList());

        return new PageImpl<>(ads, PageRequest.of(Math.max(0, page), pageSize), searchHits.getTotalHits());
    }

    private void applySorting(NativeQueryBuilder queryBuilder, String sortOption, boolean hasText, GeoPoint location) {
        switch (sortOption == null ? "" : sortOption) {
            case "relevance":
                if (hasText)
                    queryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
                else if (location != null)
                    queryBuilder.withSort(s -> s.geoDistance(g -> g.field("location")
                            .location(l -> l.latlon(ll -> ll.lat(location.getLat()).lon(location.getLon())))
                            .order(SortOrder.Asc)));
                else
                    queryBuilder.withSort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)));
                break;
            case "title_asc":
                queryBuilder.withSort(s -> s.field(f -> f.field("title.keyword").order(SortOrder.Asc)));
                break;
            case "title_desc":
                queryBuilder.withSort(s -> s.field(f -> f.field("title.keyword").order(SortOrder.Desc)));
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
        // We use a bool query to match the term as a prefix in any part of the title
        NativeQuery query = new NativeQueryBuilder().withQuery(Query.of(q -> q.bool(
                        b -> b.should(s -> s.matchPhrasePrefix(m -> m.field("title").query(term)))
                                .should(s -> s.wildcard(w -> w.field("title").wildcard("*" + term.toLowerCase() + "*"))))))
                .withPageable(PageRequest.of(0, 10)).build();

        return elasticsearchOperations.search(query, Ad.class).stream().map(hit -> hit.getContent().getTitle())
                .distinct().collect(Collectors.toList());
    }

    public List<String> findAllCategories() {
        NativeQuery query = new NativeQueryBuilder().withQuery(Query.of(q -> q.matchAll(m -> m)))
                .withPageable(PageRequest.of(0, 1000)).build();
        return elasticsearchOperations.search(query, Ad.class).stream().map(hit -> hit.getContent().getCategory())
                .distinct().sorted().collect(Collectors.toList());
    }

    public boolean hasCategoryAds(String category) {
        NativeQuery query = new NativeQueryBuilder().withQuery(
                        Query.of(q -> q.term(t -> t.field("category").value(category)))).withPageable(PageRequest.of(0, 1))
                .build();
        return elasticsearchOperations.count(query, Ad.class) > 0;
    }

    public long countAllAds() {
        NativeQuery query = new NativeQueryBuilder().withQuery(Query.of(q -> q.matchAll(m -> m))).build();
        return elasticsearchOperations.count(query, Ad.class);
    }
}
