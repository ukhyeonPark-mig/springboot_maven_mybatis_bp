package com.example.bp.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import com.example.bp.support.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Object storage (PRD §4.1): two logical disks — public (browser-loadable via
 * custom domain) and private (presigned URLs). Backed by Cloudflare R2 (S3 API)
 * when configured; otherwise falls back to the local filesystem so dev works
 * without R2 credentials. Public assets are served at {@code /storage/**} in
 * fallback mode (see WebConfig).
 */
@Service
public class R2StorageService {

    private static final Duration DEFAULT_PRESIGN_TTL = Duration.ofMinutes(15);

    private final AppProperties.R2 cfg;
    private final Path localPublicDir;
    private final Path localPrivateDir;
    private final boolean r2Enabled;

    private volatile S3Client s3;
    private volatile S3Presigner presigner;

    public R2StorageService(AppProperties properties,
                            @Value("${app.storage.local-dir:storage/app}") String localDir) {
        this.cfg = properties.r2();
        Path base = Paths.get(localDir).toAbsolutePath().normalize();
        this.localPublicDir = base.resolve("public");
        this.localPrivateDir = base.resolve("private");
        this.r2Enabled = cfg != null
                && StringUtils.hasText(cfg.endpoint())
                && StringUtils.hasText(cfg.publicBucket())
                && StringUtils.hasText(cfg.accessKeyId())
                && StringUtils.hasText(cfg.secretAccessKey());
    }

    public boolean isR2Enabled() {
        return r2Enabled;
    }

    public Path localPublicDir() {
        return localPublicDir;
    }

    /** Base URL for public assets: the R2 custom domain, or {@code /storage} locally. */
    public String publicBaseUrl() {
        if (r2Enabled && StringUtils.hasText(cfg.publicUrl())) {
            return stripTrailingSlash(cfg.publicUrl());
        }
        return "/storage";
    }

    public String publicUrl(String key) {
        return publicBaseUrl() + "/" + key;
    }

    public void putPublic(String key, byte[] bytes, String contentType) {
        if (r2Enabled) {
            s3().putObject(PutObjectRequest.builder()
                    .bucket(cfg.publicBucket()).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(bytes));
        } else {
            writeLocal(localPublicDir.resolve(key), bytes);
        }
    }

    public void deletePublic(String key) {
        if (r2Enabled) {
            s3().deleteObject(DeleteObjectRequest.builder().bucket(cfg.publicBucket()).key(key).build());
        } else {
            deleteLocal(localPublicDir.resolve(key));
        }
    }

    public void putPrivate(String key, byte[] bytes, String contentType) {
        if (r2Enabled && StringUtils.hasText(cfg.privateBucket())) {
            s3().putObject(PutObjectRequest.builder()
                    .bucket(cfg.privateBucket()).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(bytes));
        } else {
            writeLocal(localPrivateDir.resolve(key), bytes);
        }
    }

    public byte[] getPrivate(String key) {
        if (r2Enabled && StringUtils.hasText(cfg.privateBucket())) {
            return s3().getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(cfg.privateBucket()).key(key).build()).asByteArray();
        }
        try {
            return Files.readAllBytes(localPrivateDir.resolve(key));
        } catch (IOException e) {
            throw new StorageException("Failed to read private object: " + key, e);
        }
    }

    public void deletePrivate(String key) {
        if (r2Enabled && StringUtils.hasText(cfg.privateBucket())) {
            s3().deleteObject(DeleteObjectRequest.builder().bucket(cfg.privateBucket()).key(key).build());
        } else {
            deleteLocal(localPrivateDir.resolve(key));
        }
    }

    /** Time-limited URL for a private object (default 15 min, PRD §9). */
    public String presignedUrl(String key, Duration ttl) {
        if (r2Enabled && StringUtils.hasText(cfg.privateBucket())) {
            GetObjectRequest get = GetObjectRequest.builder().bucket(cfg.privateBucket()).key(key).build();
            return presigner().presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(ttl != null ? ttl : DEFAULT_PRESIGN_TTL)
                    .getObjectRequest(get).build()).url().toString();
        }
        // Local fallback: no presigning; admin-only download endpoint serves the bytes.
        return "/admin/development/backup/download/" + key;
    }

    // ── internals ─────────────────────────────────────────────────────────
    private S3Client s3() {
        if (s3 == null) {
            synchronized (this) {
                if (s3 == null) {
                    s3 = S3Client.builder()
                            .endpointOverride(URI.create(cfg.endpoint()))
                            .region(Region.of("auto"))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(cfg.accessKeyId(), cfg.secretAccessKey())))
                            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                            .build();
                }
            }
        }
        return s3;
    }

    private S3Presigner presigner() {
        if (presigner == null) {
            synchronized (this) {
                if (presigner == null) {
                    presigner = S3Presigner.builder()
                            .endpointOverride(URI.create(cfg.endpoint()))
                            .region(Region.of("auto"))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(cfg.accessKeyId(), cfg.secretAccessKey())))
                            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                            .build();
                }
            }
        }
        return presigner;
    }

    private void writeLocal(Path target, byte[] bytes) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new StorageException("Failed to write local object: " + target, e);
        }
    }

    private void deleteLocal(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new StorageException("Failed to delete local object: " + target, e);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
