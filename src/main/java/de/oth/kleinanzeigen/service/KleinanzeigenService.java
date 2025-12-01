package de.oth.kleinanzeigen.service;

import de.oth.kleinanzeigen.model.Ad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class KleinanzeigenService {

    // TODO MongoTemplate or MongoRepository ??
    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Ad> findRandomAds(int n) {
        BasicQuery query = new BasicQuery("{}", "{}");
        List<Ad> ads = mongoTemplate.find(query, Ad.class);

        // swap n randomly picked elements to the end of the list
        List<Ad> copy = new ArrayList<>(ads);
        Collections.shuffle(copy);

        // take the last n elements
        return copy.subList(0, Math.min(n, copy.size()));
    }

    public Page<Ad> findAdsByCategory(String category, int page, int pageSize) {
        return findAds(null, category, null, 0, page, pageSize);
    }

    public Page<Ad> findAds(
            String searchTitle, String category,
            GeoJsonPoint location, int radius,
            int page, int pageSize) {
        // TODO use Query and Criteria to prevent SQL-Injection ??

        List<String> selections = new ArrayList<>();

        if(searchTitle != null && !searchTitle.isBlank()) {
            if(location == null) selections.add(String.format("$text: {$search: '%s'}",  searchTitle));
            else selections.add(String.format("title: {$regex: /%s/, $options: \"i\"}",  searchTitle));
        }
        if(category != null && !category.isBlank())
            selections.add(String.format("category: {$regex: /^%s$/}", category));
        if(location != null)
            selections.add(String.format(Locale.US,
                    "location: {$near: { $geometry: { type: \"Point\", coordinates: [%f, %f]}, $maxDistance: %d}}",
                                        location.getX(), location.getY(), radius*1000));

        String queryStr = String.format("{%s}", String.join(", ", selections));
        System.out.println("queryStr: " + queryStr);

        BasicQuery contentQuery = new BasicQuery(queryStr, "{}");
        contentQuery.with(Sort.by(Sort.Direction.DESC, "publishedAt"))
                .skip(page*pageSize)
                .limit(pageSize);
        List<Ad> ads = mongoTemplate.find(contentQuery, Ad.class);

        // total result for pagination
        BasicQuery countQuery = new BasicQuery(queryStr, "{}");
        // use find instead of count because count does not work with $near
        long totalElements = mongoTemplate.find(countQuery, Ad.class).size();
        System.out.println("totalElements: " + totalElements);

        Pageable  pageable = PageRequest.of(page, pageSize);
        return new PageImpl<>(ads, pageable, totalElements);
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
}
