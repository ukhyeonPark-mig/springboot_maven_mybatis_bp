package com.example.bp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Branding assets on disk (PRD §FR-10.4 / §15.4). Mirrors the reference's
 * {@code public/branding/} directory. PR9 writes logos/favicons here; for now
 * {@link #exists} drives the navbar/sidebar logo-vs-text fallback.
 */
@Service
public class BrandingService {

    private final Path dir;

    public BrandingService(@Value("${app.branding.dir:storage/branding}") String dir) {
        this.dir = Paths.get(dir).toAbsolutePath().normalize();
    }

    public Path dir() {
        return dir;
    }

    public boolean exists(String filename) {
        return Files.isRegularFile(dir.resolve(filename));
    }

    /** Write a branding asset (logo/favicon) into the branding directory (FR-10.4). */
    public void write(String filename, byte[] bytes) {
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write branding asset: " + filename, e);
        }
    }
}
