package com.example.bp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 디스크에 저장되는 브랜딩 에셋 (PRD §FR-10.4 / §15.4). 원본의
 * {@code public/branding/} 디렉터리를 그대로 따른다. PR9에서 로고/favicon을 여기에
 * 기록한다. 현재로서는 {@link #exists}가 navbar/sidebar의 로고-vs-텍스트 폴백을 결정한다.
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

    /** 브랜딩 에셋(로고/favicon)을 브랜딩 디렉터리에 기록한다 (FR-10.4). */
    public void write(String filename, byte[] bytes) {
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write branding asset: " + filename, e);
        }
    }
}
