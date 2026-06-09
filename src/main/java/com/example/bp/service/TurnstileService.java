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
 * Cloudflare Turnstile (PRD §4.3), {@code App\Support\Turnstile}의 포팅 버전.
 * <ul>
 *   <li>local 프로파일 → {@link #enabled()} false, {@link #verify}는 항상 true</li>
 *   <li>prod에서 secret이 비어 있으면 → verify true (fail-open)</li>
 *   <li>그 외에는 siteverify를 호출</li>
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

    /** 위젯을 렌더링해야 하는지 여부 (prod + 두 키가 모두 설정됨). */
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
            return true; // fail-open (원본과 동일)
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
