package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.repository.AdRepository;
import de.oth.kleinanzeigen.service.GridFsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

@Controller
public class WebController {

    private final AdRepository adRepository;
    private final GridFsService gridFsService;

    public WebController(AdRepository adRepository, GridFsService gridFsService) {
        this.adRepository = adRepository;
        this.gridFsService = gridFsService;
    }

    @GetMapping("/ads/new")
    public String newAdForm(Model model, @CookieValue(name = "userId", required = false) String userId) {
        if (userId == null) {
            return "redirect:/"; // Redirect to home to login
        }
        model.addAttribute("ad", new Ad());
        return "ad-create";
    }

    @PostMapping("/ads/new")
    public String createAd(Ad ad, @RequestParam("file") MultipartFile file,
            @CookieValue(name = "userId", required = false) String userId) throws IOException {
        if (userId == null) {
            return "redirect:/"; // Should not happen if form is protected
        }
        ad.setCreatorId(userId);
        ad.setPublishedAt(LocalDateTime.now());

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
            model.addAttribute("isOwner", userId != null && userId.equals(ad.getCreatorId()));
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
            if (userId != null && userId.equals(ad.getCreatorId())) {
                model.addAttribute("ad", ad);
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
            if (userId != null && userId.equals(ad.getCreatorId())) {
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
            if (userId != null && userId.equals(ad.getCreatorId())) {
                adRepository.deleteById(id);
            }
        }
        return "redirect:/";
    }
}
