package com.example.bp.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 관리자 진입점 (FR-7). 요약 카드는 추후 추가 예정이며, 현재는 자리표시자입니다. */
@Controller
public class AdminDashboardController {

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }
}
