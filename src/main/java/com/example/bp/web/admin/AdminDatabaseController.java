package com.example.bp.web.admin;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 읽기 전용 데이터베이스 브라우저 (FR-11.4) — 테이블 목록 + 상위 100개 행. {@code local}
 * 프로파일로 제한됩니다(사이드바는 로컬에서만 이 항목을 표시). DDL
 * 및 행 쓰기는 의도적으로 지원하지 않습니다(스키마는 Flyway가 관리).
 */
@Controller
@Profile("local")
public class AdminDatabaseController {

    private static final int ROW_LIMIT = 100;

    private final JdbcTemplate jdbcTemplate;

    public AdminDatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/development/database")
    public String database(@RequestParam(required = false) String table, Model model) {
        List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
        model.addAttribute("tables", tables);

        if (table != null && tables.contains(table)) {
            // 안전함: 식별자를 보간하기 전에 실제 테이블 목록과 대조하여 검증함.
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM `" + table + "` LIMIT " + ROW_LIMIT);
            model.addAttribute("selectedTable", table);
            model.addAttribute("rows", rows);
            model.addAttribute("columns", rows.isEmpty() ? List.of() : rows.get(0).keySet());
        }
        return "admin/development/database";
    }
}
