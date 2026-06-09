package com.example.bp.config;

import com.example.bp.service.BrandingService;
import com.example.bp.service.R2StorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 업로드된 브랜딩 자산을 {@code /branding/**} 경로로 제공하고, R2가 구성되지 않은
 * 경우(로컬 개발) 공개 storage 객체(예: 프로필 이미지)를 로컬 파일시스템에서
 * {@code /storage/**} 경로로 제공한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BrandingService brandingService;
    private final R2StorageService storageService;

    public WebConfig(BrandingService brandingService, R2StorageService storageService) {
        this.brandingService = brandingService;
        this.storageService = storageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/branding/**")
                .addResourceLocations(brandingService.dir().toUri().toString());
        if (!storageService.isR2Enabled()) {
            registry.addResourceHandler("/storage/**")
                    .addResourceLocations(storageService.localPublicDir().toUri().toString());
        }
    }
}
