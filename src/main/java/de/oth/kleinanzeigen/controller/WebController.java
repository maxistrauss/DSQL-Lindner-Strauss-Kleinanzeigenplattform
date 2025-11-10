package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.model.Seller;
import de.oth.kleinanzeigen.repository.AdRepository;
import de.oth.kleinanzeigen.service.GridFsService;
import de.oth.kleinanzeigen.service.KleinanzeigenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private static final int PAGE_SIZE = 3;

    public WebController(AdRepository adRepository, GridFsService gridFsService, KleinanzeigenService kleinanzeigenService) {
        this.adRepository = adRepository;
        this.gridFsService = gridFsService;
        this.kleinanzeigenService = kleinanzeigenService;
    }

    @GetMapping("/ads/new")
    public String newAdForm(Model model, @CookieValue(name = "userId", required = false) String userId) {
        if (userId == null) {
            return "redirect:/"; // Redirect to home to login
        }
        model.addAttribute("ad", new Ad());
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

    @GetMapping("/test")
    public String index(Model model) {
        model.addAttribute("message", "Thymeleaf funktioniert!");
        return "index";
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
            model.addAttribute("userId", userId); // Add userId to the model
            return "ad-details";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/ads/{id}/comments")
    public String addComment(@PathVariable String id, @RequestParam String text, @RequestParam int rating,
            @RequestParam(name = "guestName", required = false) String guestName,
            @CookieValue(name = "userId", required = false) String loggedInUserId) {
        Optional<Ad> adOptional = adRepository.findById(id);
        if (adOptional.isPresent()) {
            Ad ad = adOptional.get();

            Comment comment = new Comment();
            comment.setText(text);
            comment.setRating(rating);
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

    @GetMapping("/search")
    public String search(
            Model model,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @CookieValue(name = "userId", required = false) String userId){

        Page<Ad> adsPage = kleinanzeigenService.findAds(title, category, page, PAGE_SIZE);

        model.addAttribute("userId", userId); // Add userId to the model
        model.addAttribute("title", title);
        model.addAttribute("category", category);
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

        System.out.println(page);
        System.out.println(adsPage.getTotalPages());
        System.out.println(adsPage.getTotalElements());
        return "search";
    }
}
