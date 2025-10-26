package de.oth.kleinanzeigen;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedList;
import java.util.List;

@Controller
class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        /*
        List<Ad> randomAds = adService.getRandomAds(5);
        //List<Category> categories = categoryService.getAllSorted();
        List<String> categories = categoryService.getAllSorted();
        long categoryCount = categoryService.getCount();
        */

        List<Ad> randomAds = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            randomAds.add(new Ad("Ad " + i, "TEST " + i, i + 0.38));
        }
        List<String> categories = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            categories.add((char)('A' + i) + "");
        }
        long categoryCount = categories.size();

        model.addAttribute("randomAds", randomAds);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCount", categoryCount);

        return "home";
    }
}
