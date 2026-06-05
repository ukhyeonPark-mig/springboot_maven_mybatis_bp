package com.example.bp.web.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Public landing page (FR-1). Fleshed out in PR5. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "home/index";
    }
}
