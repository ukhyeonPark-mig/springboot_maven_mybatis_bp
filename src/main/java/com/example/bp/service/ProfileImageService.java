package com.example.bp.service;

import java.io.IOException;
import java.util.Set;

import com.example.bp.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shared profile-image pipeline (PRD §FR-8.2) used by both the client area and
 * the admin account: validate → WebP 100/400 → public storage → replace + delete
 * the previous file. Korean validation messages match the reference.
 */
@Service
public class ProfileImageService {

    public static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/jpg", "image/png");
    private static final String K100 = "user/profile_image/100/";
    private static final String K400 = "user/profile_image/400/";

    private final ImageService imageService;
    private final R2StorageService storage;
    private final UserService userService;

    public ProfileImageService(ImageService imageService, R2StorageService storage, UserService userService) {
        this.imageService = imageService;
        this.storage = storage;
        this.userService = userService;
    }

    /** Validation error message, or {@code null} if the upload is acceptable. */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "이미지를 선택해 주세요.";
        }
        if (file.getSize() > MAX_BYTES) {
            return "이미지 크기는 2MB를 초과할 수 없습니다.";
        }
        if (file.getContentType() == null || !ALLOWED.contains(file.getContentType().toLowerCase())) {
            return "이미지를 처리할 수 없습니다. JPEG 또는 PNG 파일인지 확인해 주세요.";
        }
        return null;
    }

    public void replace(Long userId, MultipartFile file) throws IOException {
        byte[] input = file.getBytes();
        byte[] webp100 = imageService.toWebpCover(input, 100, 100, ImageService.PROFILE_WEBP_QUALITY);
        byte[] webp400 = imageService.toWebpCover(input, 400, 400, ImageService.PROFILE_WEBP_QUALITY);

        String filename = "user_" + userId + "_" + (System.currentTimeMillis() / 1000) + ".webp";
        storage.putPublic(K100 + filename, webp100, "image/webp");
        storage.putPublic(K400 + filename, webp400, "image/webp");

        User user = userService.findById(userId);
        String previous = user.getProfileImage();
        userService.updateProfileImage(userId, filename);
        if (previous != null) {
            storage.deletePublic(K100 + previous);
            storage.deletePublic(K400 + previous);
        }
    }

    /** @return true if an image existed and was removed. */
    public boolean remove(Long userId) {
        User user = userService.findById(userId);
        if (user.getProfileImage() == null) {
            return false;
        }
        storage.deletePublic(K100 + user.getProfileImage());
        storage.deletePublic(K400 + user.getProfileImage());
        userService.updateProfileImage(userId, null);
        return true;
    }
}
