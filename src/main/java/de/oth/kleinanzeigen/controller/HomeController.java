package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.service.KleinanzeigenService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final KleinanzeigenService kleinanzeigenService;

    public HomeController(KleinanzeigenService kleinanzeigenService) {
        this.kleinanzeigenService = kleinanzeigenService;
    }

    @GetMapping("/")
    public String home(Model model, @CookieValue(name = "userId", required = false) String userId) {
        model.addAttribute("userId", userId);

        List<Ad> randomAds = kleinanzeigenService.findRandomAds(5);
        List<String> categories = kleinanzeigenService.findAllCategories();
        long categoryCount = categories.size();

        model.addAttribute("randomAds", randomAds);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCount", categoryCount);

        long adCount = kleinanzeigenService.countAllAds();
        model.addAttribute("adCount", adCount);

        return "home";
    }
}
