package com.example.bp.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed application settings bound from {@code app.*} (PRD §11).
 * Relaxed binding maps e.g. {@code app.r2.public-bucket} -> {@code r2.publicBucket}.
 */
@ConfigurationProperties("app")
public record AppProperties(
        String name,
        String url,
        R2 r2,
        Turnstile turnstile,
        Ses ses,
        Mail mail,
        Contact contact
) {
    public record R2(
            String accessKeyId,
            String secretAccessKey,
            String endpoint,
            String publicBucket,
            String publicUrl,
            String privateBucket
    ) {}

    public record Turnstile(String siteKey, String secretKey) {}

    public record Ses(String accessKeyId, String secretAccessKey, String region) {}

    public record Mail(String fromAddress, String fromName) {}

    public record Contact(String toAddress, String toName) {}
}
