package com.example.bp.web.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 공개 랜딩 페이지 (FR-1). PR5에서 구체화됨. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "home/index";
    }
}
