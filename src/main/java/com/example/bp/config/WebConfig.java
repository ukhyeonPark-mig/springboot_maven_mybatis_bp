package com.example.bp.config;

import com.example.bp.service.BrandingService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded branding assets from the filesystem at {@code /branding/**}. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BrandingService brandingService;

    public WebConfig(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/branding/**")
                .addResourceLocations(brandingService.dir().toUri().toString());
    }
}
