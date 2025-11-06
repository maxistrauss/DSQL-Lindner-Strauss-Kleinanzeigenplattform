package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.model.Comment;
import de.oth.kleinanzeigen.service.KleinanzeigenService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
class CategoryController {

    private final KleinanzeigenService kleinanzeigenService;
    private static final int PAGE_SIZE = 3;

    public CategoryController(KleinanzeigenService kleinanzeigenService) {
        this.kleinanzeigenService = kleinanzeigenService;
    }

    @GetMapping("/categories/{category}")
    public String categoriesName(@PathVariable String category, Model model,
                            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                            @CookieValue(name = "userId", required = false) String userId) {
        boolean categoryHasAds = kleinanzeigenService.hasCategoryAds(category);
        if (categoryHasAds) {
            Page<Ad> adsPage = kleinanzeigenService.findAdsByCategory(category, page, PAGE_SIZE);

            model.addAttribute("userId", userId); // Add userId to the model
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

            return "categories";
        } else {
            return "redirect:/";
        }
    }
}
