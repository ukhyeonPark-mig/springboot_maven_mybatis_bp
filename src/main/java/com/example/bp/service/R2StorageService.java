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
 * 파일(이미지·백업 등)을 저장/조회하는 오브젝트 스토리지 서비스 (PRD §4.1).
 *
 * 저장 공간을 두 종류로 나눈다:
 *   - public  : 누구나 URL로 접근 가능 (예: 프로필 이미지). 커스텀 도메인으로 노출.
 *   - private : 비공개. 접근 시 15분짜리 임시 서명 URL(presigned URL)을 발급해야만 받을 수 있음.
 *
 * 실제 저장 위치는 설정 유무에 따라 자동으로 갈린다:
 *   - R2 설정이 있으면  → Cloudflare R2에 업로드
 *   - 설정이 없으면     → 로컬 파일시스템({@code storage/app})에 저장 → R2 키 없이도 개발 가능
 *     (이 폴백 모드에서 public 파일은 {@code /storage/**} 경로로 제공된다. WebConfig 참고)
 *
 * 참고 - "R2인데 왜 S3 SDK?": R2는 AWS S3와 "같은 API"를 쓰도록 만들어진 호환 스토리지라,
 * AWS S3 SDK를 그대로 쓰되 접속 주소(endpoint)만 R2로 바꿔주면 동작한다 (아래 s3() 참고).
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

    /** public 파일을 받을 수 있는 기본 URL. R2면 커스텀 도메인, 로컬 폴백이면 {@code /storage}. */
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

    /** private 파일을 한시적으로 받을 수 있는 임시 서명 URL을 발급한다 (기본 15분 후 만료, PRD §9). */
    public String presignedUrl(String key, Duration ttl) {
        if (r2Enabled && StringUtils.hasText(cfg.privateBucket())) {
            GetObjectRequest get = GetObjectRequest.builder().bucket(cfg.privateBucket()).key(key).build();
            return presigner().presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(ttl != null ? ttl : DEFAULT_PRESIGN_TTL)
                    .getObjectRequest(get).build()).url().toString();
        }
        // 로컬 폴백: 서명 URL 개념이 없으므로, 관리자 전용 다운로드 엔드포인트로 대신 받게 한다.
        return "/admin/development/backup/download/" + key;
    }

    // ── 내부 구현 ─────────────────────────────────────────────────────────
    // AWS S3 SDK로 R2에 접속한다. endpointOverride로 접속 주소를 R2로 바꾸는 게 핵심이며,
    // R2는 리전 개념이 없어 "auto", 주소는 path-style을 쓴다. 최초 사용 시에만 생성(지연 초기화).
    private S3Client s3() {
        if (s3 == null) {
            synchronized (this) {
                if (s3 == null) {
                    s3 = S3Client.builder()
                            .endpointOverride(URI.create(cfg.endpoint())) // AWS가 아닌 R2 주소로
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
