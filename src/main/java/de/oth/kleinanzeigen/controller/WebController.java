package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.dto.ApiAdResult;
import de.oth.kleinanzeigen.dto.ApiSearchResult;
import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.Seller;
import de.oth.kleinanzeigen.repository.AdRepository;
import de.oth.kleinanzeigen.service.FederatedSearchService;
import de.oth.kleinanzeigen.service.GeoServiceOSM;
import de.oth.kleinanzeigen.service.GridFsService;
import de.oth.kleinanzeigen.service.KleinanzeigenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class WebController {

    private final AdRepository adRepository;
    private final GridFsService gridFsService;
    private final KleinanzeigenService kleinanzeigenService;
    private final GeoServiceOSM geoService;
    private final FederatedSearchService federatedSearchService;
    private final de.oth.kleinanzeigen.service.TrackingService trackingService;
    private static final int PAGE_SIZE = 3;
    private static final List<Integer> RADIUS_OPTIONS = Arrays.asList(1,5,10,25,50,100,250,500);

    public WebController(AdRepository adRepository, GridFsService gridFsService,
            KleinanzeigenService kleinanzeigenService, GeoServiceOSM geoService,
            FederatedSearchService federatedSearchService,
            de.oth.kleinanzeigen.service.TrackingService trackingService) {
        this.adRepository = adRepository;
        this.gridFsService = gridFsService;
        this.kleinanzeigenService = kleinanzeigenService;
        this.geoService = geoService;
        this.federatedSearchService = federatedSearchService;
        this.trackingService = trackingService;
    }

    @GetMapping(value = "/", params = "mode=api_search")
    @ResponseBody
    public ApiSearchResult apiSearch(@RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "lon", required = false) Double lon,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "distance", required = false) Integer distance, HttpServletRequest request) {
        System.out.println("API search request received. Search term: " + search + ", Lon: " + lon + ", Lat: " + lat
                + ", Distance: " + distance);

        GeoJsonPoint location = null;
        if (lon != null && lat != null) {
            location = new GeoJsonPoint(lon, lat);
        }
        int r = distance != null ? distance : -1;

        Page<Ad> ads = kleinanzeigenService.findAds(search, null, location, r, 0, 20);

        // Tracking
        String locString = (lon != null && lat != null) ? lat + ", " + lon : null;
        trackingService.logSearch(search, locString, distance, ads.getNumberOfElements(), request.getRemoteAddr());

        List<ApiAdResult> results = ads.getContent().stream().map(ad -> {
            String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/ads/").path(ad.getId())
                    .toUriString();
            return new ApiAdResult(ad.getTitle(), ad.getPrice(), url);
        }).collect(Collectors.toList());

        return new ApiSearchResult("OTH-Kleinanzeigen-Portal", results);
    }

    @GetMapping("/")
    public String home(
            Model model,
            @CookieValue(name = "userId", required = false) String userId) {
        model.addAttribute("userId", userId);

        List<Ad> randomAds = kleinanzeigenService.findRandomAds(5);
        List<String> categories = kleinanzeigenService.findAllCategories();
        long categoryCount = categories.size();

        model.addAttribute("randomAds", randomAds);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCount", categoryCount);
        model.addAttribute("radiusOptions", RADIUS_OPTIONS);

        long adCount = kleinanzeigenService.countAllAds();
        model.addAttribute("adCount", adCount);

        List<de.oth.kleinanzeigen.dto.TopSearchTerm> topSearchTerms = trackingService.getTopSearchTerms(10);
        model.addAttribute("topSearchTerms", topSearchTerms);

        return "home";
    }

    @GetMapping("/search")
    public String search(
            Model model,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "radius", required = false, defaultValue = "-1") int radius,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @CookieValue(name = "userId", required = false) String userId, HttpServletResponse response,
            HttpServletRequest request) {
        System.out.println(
                "Web search request received. Title: " + title + ", Category: " + category + ", Address: " + address
                        + ", Radius: " + radius + ", Page: " + page);

        GeoJsonPoint location = null;
        if(address != null && !address.isBlank()){
            location = geoService.geocode(address);

            // address cookie
            Cookie cookie = new Cookie("userAddress", address);
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour
            response.addCookie(cookie);
            // radius cookie
            cookie = new Cookie("userRadius", radius + "");
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour
            response.addCookie(cookie);
        }
        Page<Ad> adsPage = kleinanzeigenService.findAds(title, category, location, radius, page, PAGE_SIZE);

        // Tracking
        if (page == 0) { // Log only first page request
            Integer dist = radius > 0 ? radius : null;
            trackingService.logSearch(title, address, dist, (int) adsPage.getTotalElements(), request.getRemoteAddr());
        }

        if (page == 0) {
            Double lon = location != null ? location.getX() : null;
            Double lat = location != null ? location.getY() : null;
            Integer dist = radius > 0 ? radius : null;
            List<ApiSearchResult> externalResults = federatedSearchService.searchExternalPortals(title.trim(), lon, lat,
                    dist);
            model.addAttribute("externalResults", externalResults);
        }

        model.addAttribute("userId", userId); // Add userId to the model
        model.addAttribute("title", title);
        model.addAttribute("category", category);
        model.addAttribute("address", address);
        model.addAttribute("radius", radius);
        model.addAttribute("radiusOptions", RADIUS_OPTIONS);
        model.addAttribute("ads", adsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", adsPage.getTotalPages());
        model.addAttribute("totalElements", adsPage.getTotalElements());

        // list of page indices
        if (adsPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.range(0, adsPage.getTotalPages())
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        } else {
            model.addAttribute("pageNumbers", Collections.emptyList());
        }

        return "search";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("userId", username);
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour
        response.addCookie(cookie);

        String referer = request.getHeader("Referer");
        if(referer != null && !referer.isEmpty()){
            return "redirect:"+referer;
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("userId", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        String referer = request.getHeader("Referer");
        if(referer != null && !referer.isEmpty()){
            return "redirect:"+referer;
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/ads/new")
    public String newAdForm(Model model, @CookieValue(name = "userId", required = false) String userId) {
        if (userId == null) {
            return "redirect:/"; // Redirect to home to login
        }
        model.addAttribute("ad", new Ad());
        model.addAttribute("radiusOptions", RADIUS_OPTIONS);
        model.addAttribute("userId", userId); // Add userId to the model
        return "ad-create";
    }

    @PostMapping("/ads/new")
    public String createAd(Ad ad, @RequestParam("file") MultipartFile file,
            @CookieValue(name = "userId", required = false) String userId) throws IOException {
        if (userId == null) {
            return "redirect:/"; // Should not happen if form is protected
        }
        ad.setPublishedAt(Instant.now());

        Seller seller = new Seller();
        seller.setUsername(userId);
        seller.setPhoneNumber("0123456789");
        ad.setSeller(seller);

        if (!file.isEmpty()) {
            String imageId = gridFsService.store(file);
            ad.setImageId(imageId);
        }

        Ad savedAd = adRepository.save(ad);
        return "redirect:/ads/" + savedAd.getId();
    }

    @GetMapping("/ads/{id}")
    public String adDetails(@PathVariable String id, Model model,
            @CookieValue(name = "userId", required = false) String userId) {
        Optional<Ad> adOptional = adRepository.findById(id);
        if (adOptional.isPresent()) {
            Ad ad = adOptional.get();
            if (ad.getComments() != null) {
                ad.getComments().sort(Comparator.comparing(Comment::getCreatedAt).reversed());
            }
            model.addAttribute("ad", ad);
            model.addAttribute("isOwner", userId != null && userId.equals(ad.getSeller().getUsername()));
            model.addAttribute("radiusOptions", RADIUS_OPTIONS);
            model.addAttribute("userId", userId); // Add userId to the model
            return "ad-details";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/ads/{id}/comments")
    public String addComment(@PathVariable String id, @RequestParam String text,
            @RequestParam(name = "guestName", required = false) String guestName,
            @CookieValue(name = "userId", required = false) String loggedInUserId) {
        Optional<Ad> adOptional = adRepository.findById(id);
        if (adOptional.isPresent()) {
            Ad ad = adOptional.get();

            Comment comment = new Comment();
            comment.setText(text);
            comment.setCreatedAt(LocalDateTime.now());

            String commenterName;
            if (loggedInUserId != null && !loggedInUserId.isEmpty()) {
                commenterName = loggedInUserId; // Use the logged-in user's name
            } else {
                commenterName = (guestName != null && !guestName.isEmpty())
                        ? guestName
                        : "Anonymous"; // Use guest name or Anonymous
            }
            comment.setUserId(commenterName);

            if (ad.getComments() == null) {
                ad.setComments(new ArrayList<>());
            }
            ad.getComments().add(comment);
            adRepository.save(ad);

            return "redirect:/ads/" + id;
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/ads/{id}/edit")
    public String editAdForm(@PathVariable String id, Model model,
            @CookieValue(name = "userId", required = false) String userId) {
        Optional<Ad> adOptional = adRepository.findById(id);
        if (adOptional.isPresent()) {
            Ad ad = adOptional.get();
            if (userId != null && userId.equals(ad.getSeller().getUsername())) {
                model.addAttribute("ad", ad);
                model.addAttribute("radiusOptions", RADIUS_OPTIONS);
                model.addAttribute("userId", userId); // Add userId to the model
                return "ad-edit";
            }
        }
        return "redirect:/";
    }

    @PostMapping("/ads/{id}/edit")
    public String updateAd(@PathVariable String id, Ad adData, @RequestParam("file") MultipartFile file,
            @CookieValue(name = "userId", required = false) String userId) throws IOException {
        Optional<Ad> adOptional = adRepository.findById(id);
        if (adOptional.isPresent()) {
            Ad ad = adOptional.get();
            if (userId != null && userId.equals(ad.getSeller().getUsername())) {
                ad.setTitle(adData.getTitle());
                ad.setDescription(adData.getDescription());
                ad.setPrice(adData.getPrice());
                ad.setCategory(adData.getCategory());

                if (!file.isEmpty()) {
                    String imageId = gridFsService.store(file);
                    ad.setImageId(imageId);
                }

                adRepository.save(ad);
                return "redirect:/ads/" + id;
            }
        }
        return "redirect:/";
    }

    @PostMapping("/ads/{id}/delete")
    public String deleteAd(@PathVariable String id, @CookieValue(name = "userId", required = false) String userId) {
        Optional<Ad> adOptional = adRepository.findById(id);
        if (adOptional.isPresent()) {
            Ad ad = adOptional.get();
            if (userId != null && userId.equals(ad.getSeller().getUsername())) {
                adRepository.deleteById(id);
            }
        }
        return "redirect:/";
    }
}
