package com.example.bp.web.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public legal pages (FR-4): render the stored {@code settings.privacy} /
 * {@code settings.terms} HTML (admin-authored, rendered with {@code th:utext}).
 * The {@code setting} model attribute is supplied by GlobalModelAttributes.
 */
@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy() {
        return "home/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "home/terms";
    }
}
