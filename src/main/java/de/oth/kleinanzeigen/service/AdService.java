package de.oth.kleinanzeigen.service;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.elastic.AdSearchDocument;
import de.oth.kleinanzeigen.repository.mongo.AdRepository;
import de.oth.kleinanzeigen.repository.elastic.AdSearchRepository;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.NoSuchElementException;

@Service
public class AdService {

    private final AdRepository adRepository;
    private final AdSearchRepository searchRepository;

    public AdService(AdRepository adRepository, AdSearchRepository searchRepository) {
        this.adRepository = adRepository;
        this.searchRepository = searchRepository;
    }

    @Transactional
    public Ad createAd(Ad ad) {
        ad.setPublishedAt(Instant.now());

        if(ad.getComments() == null) {
            ad.setComments(new ArrayList<>());
        }

        Ad saved = adRepository.save(ad);

        // index synchronously
        AdSearchDocument sd = toSearchDoc(saved);
        searchRepository.save(sd);

        return saved;
    }

    @Transactional
    public Ad updateAd(String adId, Ad update) {
        Ad ad = adRepository.findById(adId).orElseThrow(() -> new NoSuchElementException("Ad not found: " + adId));

        if(ad.getComments() == null) {
            ad.setComments(new ArrayList<>());
        }

        ad.setTitle(update.getTitle());
        ad.setDescription(update.getDescription());
        ad.setPrice(update.getPrice());
        ad.setCategory(update.getCategory());
        ad.setImageId(update.getImageId());
        ad.setLocation(update.getLocation());
        ad.setAddress(update.getAddress());

        Ad saved = adRepository.save(ad);

        AdSearchDocument sd = toSearchDoc(saved);
        searchRepository.save(sd);

        return saved;
    }

    @Transactional
    public void deleteAd(String id) {
        adRepository.deleteById(id);
        searchRepository.deleteById(id);
    }

    // comment create must update counter in elastic
    @Transactional
    public Comment addComment(String adId, Comment comment) {
        Ad ad = adRepository.findById(adId).orElseThrow(() -> new NoSuchElementException("Ad not found: " + adId));

        if(ad.getComments() == null) {
            ad.setComments(new ArrayList<>());
        }

        comment.setCreatedAt(Instant.now());
        ad.getComments().add(comment);
        Ad saved = adRepository.save(ad);

        int numberOfComments = saved.getComments().size();
        AdSearchDocument sd = toSearchDoc(saved);
        sd.setNumberOfComments(numberOfComments);
        searchRepository.save(sd);

        return comment;
    }

    public static AdSearchDocument toSearchDoc(Ad ad) {
        AdSearchDocument d = new AdSearchDocument();
        d.setId(ad.getId());
        d.setTitle(ad.getTitle());
        d.setDescription(ad.getDescription());
        d.setCategory(ad.getCategory());
        d.setPrice(ad.getPrice() != null ? ad.getPrice() : null);
        d.setPublishedAt(ad.getPublishedAt());
        d.setImageId(ad.getImageId());
        if (ad.getLocation() != null) {
            d.setLocation(new GeoPoint(ad.getLocation().getY(), ad.getLocation().getX())); // lat, lon
        }
        int numberOfComments = ad.getComments() != null ? ad.getComments().size() : 0;
        d.setNumberOfComments(numberOfComments);
        return d;
    }
}
