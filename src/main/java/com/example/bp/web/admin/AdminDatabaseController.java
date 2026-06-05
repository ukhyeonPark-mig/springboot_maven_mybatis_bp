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
 * Read-only database browser (FR-11.4) — table list + first 100 rows. Restricted
 * to the {@code local} profile (the sidebar shows this item only locally). DDL
 * and row writes are intentionally NOT supported (Flyway owns the schema).
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
            // Safe: identifier validated against the actual table list before interpolation.
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM `" + table + "` LIMIT " + ROW_LIMIT);
            model.addAttribute("selectedTable", table);
            model.addAttribute("rows", rows);
            model.addAttribute("columns", rows.isEmpty() ? List.of() : rows.get(0).keySet());
        }
        return "admin/development/database";
    }
}
