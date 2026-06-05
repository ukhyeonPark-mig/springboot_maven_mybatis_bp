package com.example.bp.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Admin entry point (FR-7). Summary cards added later; placeholder for now. */
@Controller
public class AdminDashboardController {

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }
}
