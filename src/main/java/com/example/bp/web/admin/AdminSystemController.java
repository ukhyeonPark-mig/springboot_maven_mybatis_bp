package com.example.bp.web.admin;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Runtime / environment info (FR-11.3) — the Spring-stack replacement for the
 * reference PhpInfo page. Served at the sidebar's "PhpInfo" route.
 */
@Controller
public class AdminSystemController {

    private final Environment environment;

    public AdminSystemController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/admin/development/php")
    public String system(Model model) {
        Runtime runtime = Runtime.getRuntime();
        long mb = 1024 * 1024;
        Map<String, String> info = new LinkedHashMap<>();
        info.put("Java 버전", System.getProperty("java.version"));
        info.put("JVM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
        info.put("Spring Boot", SpringBootVersion.getVersion());
        info.put("Spring Framework", SpringVersion.getVersion());
        info.put("활성 프로파일", String.join(", ", environment.getActiveProfiles()));
        info.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        info.put("가용 프로세서", String.valueOf(runtime.availableProcessors()));
        info.put("최대 메모리 (MB)", String.valueOf(runtime.maxMemory() / mb));
        info.put("사용 메모리 (MB)", String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / mb));
        info.put("업타임 (초)", String.valueOf(ManagementFactory.getRuntimeMXBean().getUptime() / 1000));
        info.put("타임존", java.util.TimeZone.getDefault().getID());
        model.addAttribute("info", info);
        return "admin/development/system";
    }
}
