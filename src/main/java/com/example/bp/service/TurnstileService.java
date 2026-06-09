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
 * 폼을 제출한 게 사람인지 봇인지 Cloudflare Turnstile(캡차)로 확인하는 서비스 (PRD §4.3).
 * 로그인·회원가입·문의에서 자동화된 봇/스팸을 걸러내는 데 쓴다.
 *
 * 동작 모드:
 *   - local(개발) 환경 → 캡차를 끈다: {@link #enabled()}는 false, {@link #verify}는 항상 통과
 *   - prod인데 secret 키가 없으면 → 검증을 건너뛰고 통과 (fail-open, 운영에선 키 설정 필수)
 *   - 그 외 → Cloudflare siteverify API로 실제 검증
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

    /** 화면에 캡차 위젯을 띄울지 여부. prod이고 siteKey·secretKey가 모두 있을 때만 true. */
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
            return true; // secret 미설정이면 검증을 건너뛰고 통과 (fail-open, 원본과 동일)
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
