package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.model.Ad;
import de.oth.kleinanzeigen.service.KleinanzeigenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

        return "home";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, HttpServletResponse response) {
        Cookie cookie = new Cookie("userId", username);
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour
        response.addCookie(cookie);
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("userId", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }
}
