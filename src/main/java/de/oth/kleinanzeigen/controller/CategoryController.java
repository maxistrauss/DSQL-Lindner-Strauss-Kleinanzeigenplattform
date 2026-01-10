package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.model.elastic.AdSearchDocument;
import de.oth.kleinanzeigen.service.KleinanzeigenService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
class CategoryController {

    private final KleinanzeigenService kleinanzeigenService;
    private static final int PAGE_SIZE = 3;
    private static final List<Integer> RADIUS_OPTIONS = Arrays.asList(1,5,10,25,50,100,250,500);

    public CategoryController(KleinanzeigenService kleinanzeigenService) {
        this.kleinanzeigenService = kleinanzeigenService;
    }

    @GetMapping("/categories/{category}")
    public String categoriesName(
            @PathVariable String category, Model model,
            @RequestParam(name = "sort", required = false, defaultValue = "relevance") String sort,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @CookieValue(name = "userId", required = false) String userId) {
        boolean categoryHasAds = kleinanzeigenService.hasCategoryAds(category);
        if (categoryHasAds) {
            Page<AdSearchDocument> adsPage = kleinanzeigenService.findAdsByCategory(category, sort, page, PAGE_SIZE);

            model.addAttribute("userId", userId); // Add userId to the model
            model.addAttribute("category", category);
            model.addAttribute("ads", adsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", adsPage.getTotalPages());
            model.addAttribute("totalElements", adsPage.getTotalElements());
            model.addAttribute("radiusOptions", RADIUS_OPTIONS);

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
