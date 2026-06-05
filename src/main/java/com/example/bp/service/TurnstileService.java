package com.example.bp.service;

import com.example.bp.support.AppProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Cloudflare Turnstile (PRD §4.3), port of {@code App\Support\Turnstile}.
 * <ul>
 *   <li>local profile → {@link #enabled()} false, {@link #verify} always true</li>
 *   <li>prod with blank secret → verify true (fail-open)</li>
 *   <li>otherwise calls siteverify</li>
 * </ul>
 */
@Service
public class TurnstileService {

    private static final String SITEVERIFY_URL =
            "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final boolean local;
    private final AppProperties.Turnstile config;
    private final RestClient http = RestClient.create();

    public TurnstileService(Environment environment, AppProperties properties) {
        this.local = environment.matchesProfiles("local");
        this.config = properties.turnstile();
    }

    /** Whether the widget should be rendered (prod + both keys set). */
    public boolean enabled() {
        if (local) {
            return false;
        }
        return StringUtils.hasText(config.siteKey()) && StringUtils.hasText(config.secretKey());
    }

    public String siteKey() {
        return config.siteKey();
    }

    public boolean verify(String token, String remoteIp) {
        if (local) {
            return true;
        }
        String secret = config.secretKey();
        if (!StringUtils.hasText(secret)) {
            return true; // fail-open (matches reference)
        }
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secret);
            form.add("response", token);
            if (StringUtils.hasText(remoteIp)) {
                form.add("remoteip", remoteIp);
            }
            SiteverifyResponse response = http.post()
                    .uri(SITEVERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteverifyResponse.class);
            return response != null && response.success();
        } catch (Exception e) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SiteverifyResponse(boolean success) {
    }
}
