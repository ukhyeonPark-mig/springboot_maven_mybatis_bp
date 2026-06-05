package com.example.bp.config;

import com.example.bp.service.BrandingService;
import com.example.bp.service.R2StorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded branding assets at {@code /branding/**} and, when R2 is not
 * configured (local dev), public storage objects (e.g. profile images) at
 * {@code /storage/**} from the local filesystem.
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
