package com.example.bp.domain;

import java.time.LocalDateTime;
import lombok.Data;

/** Singleton app settings (table {@code settings}, PRD §5.2). */
@Data
public class Setting {
    private Long id;
    private String footer;
    private String version;
    private String terms;
    private String privacy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
