package com.example.bp.web.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 최소 로그 뷰어 (FR-11.2): 설정된 로그 파일의 tail. */
@Controller
public class AdminLogsController {

    private static final int TAIL_LINES = 500;

    private final String logFile;

    public AdminLogsController(@Value("${logging.file.name:}") String logFile) {
        this.logFile = logFile;
    }

    @GetMapping("/admin/development/logs")
    public String logs(Model model) {
        model.addAttribute("logFile", logFile);
        model.addAttribute("lines", tail());
        return "admin/development/logs";
    }

    private List<String> tail() {
        if (logFile == null || logFile.isBlank()) {
            return List.of("파일 로깅이 설정되지 않았습니다. (logging.file.name 미설정 — 운영 프로파일에서 활성화됩니다.)");
        }
        Path path = Paths.get(logFile);
        if (!Files.isRegularFile(path)) {
            return List.of("로그 파일을 찾을 수 없습니다: " + path.toAbsolutePath());
        }
        try {
            List<String> all = Files.readAllLines(path);
            int from = Math.max(0, all.size() - TAIL_LINES);
            return all.subList(from, all.size());
        } catch (IOException e) {
            return List.of("로그 파일을 읽을 수 없습니다: " + e.getMessage());
        }
    }
}
