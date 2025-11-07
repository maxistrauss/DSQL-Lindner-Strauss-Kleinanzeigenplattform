package de.oth.kleinanzeigen.service;

import de.oth.kleinanzeigen.model.Ad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class KleinanzeigenService {

    // TODO MongoTemplate or MongoRepository ??
    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Ad> findRandomAds(int n) {
        BasicQuery query = new BasicQuery("{}", "{}");
        List<Ad> ads = mongoTemplate.find(query, Ad.class);
        int length = ads.size();

        // swap n randomly picked elements to the end of the list
        for(int i = length-1; i >= length-n; i--) {
            Collections.swap(ads, i, (int)(Math.random()*(i+1)));
        }

        // take the last n elements
        return ads.subList(length-n, length);
    }

    public Page<Ad> findAdsByCategory(String category, int page, int pageSize) {
        return findAds(null, category, page, pageSize);
    }

    public Page<Ad> findAdsByTitle(String searchTitle, int page, int pageSize) {
        return findAds(searchTitle, null, page, pageSize);
    }

    public Page<Ad> findAds(String searchTitle, String category, int page, int pageSize) {
        // TODO use Query and Criteria to prevent SQL-Injection ??
        if(searchTitle == null) searchTitle = ".*";
        if(category == null) category = ".*";
        String queryStr = String.format(
                            "{title: {$regex: /%s/, $options: \"i\"}, "
                            + "category: {$regex: /^%s$/}}",
                            searchTitle, category);

        BasicQuery contentQuery = new BasicQuery(queryStr, "{}");
        contentQuery.with(Sort.by(Sort.Direction.DESC, "publishedAt"))
                .skip(page*pageSize)
                .limit(pageSize);
        List<Ad> ads = mongoTemplate.find(contentQuery, Ad.class);

        // total result for pagination
        BasicQuery countQuery = new BasicQuery(queryStr);
        long totalElements = mongoTemplate.count(countQuery, Ad.class);

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
}
