package de.oth.kleinanzeigen.controller;

import de.oth.kleinanzeigen.model.Ad;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, @CookieValue(name = "userId", required = false) String userId) {
        model.addAttribute("userId", userId);

        // Dummy data for display
        List<Ad> randomAds = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            randomAds.add(
                    new Ad("Ad " + i, "TEST " + i, "Category " + i, new BigDecimal(i + 0.38), LocalDateTime.now(), null,
                            null, null, null));
        }
        List<String> categories = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            categories.add((char) ('A' + i) + "");
        }
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
