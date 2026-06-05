package com.example.bp.web.client;

import java.util.Set;

import com.example.bp.domain.User;
import com.example.bp.security.AuthSessionService;
import com.example.bp.security.SecurityPrincipal;
import com.example.bp.service.ImageService;
import com.example.bp.service.R2StorageService;
import com.example.bp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Client profile image (FR-6.1): upload → WebP 100/400 to public storage,
 * replacing and deleting the previous file; plus delete (PRD §FR-8.2 pipeline).
 */
@Controller
public class ClientProfileController {

    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/jpg", "image/png");
    private static final String KEY_100 = "user/profile_image/100/";
    private static final String KEY_400 = "user/profile_image/400/";
    protected static final String CARD = "client/profile :: card";
    protected static final String VIEW = "client/profile";

    private final UserService userService;
    private final ImageService imageService;
    private final R2StorageService storage;
    private final AuthSessionService authSession;

    public ClientProfileController(UserService userService, ImageService imageService,
                                   R2StorageService storage, AuthSessionService authSession) {
        this.userService = userService;
        this.imageService = imageService;
        this.storage = storage;
        this.authSession = authSession;
    }

    @GetMapping("/client/profile")
    public String profile() {
        return VIEW;
    }

    @PostMapping("/client/profile")
    public String save(@RequestParam(value = "profile_image", required = false) MultipartFile file,
                      @AuthenticationPrincipal SecurityPrincipal principal,
                      HttpServletRequest request, HttpServletResponse response, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "이미지를 선택해 주세요.");
            return CARD;
        }
        if (file.getSize() > MAX_BYTES) {
            model.addAttribute("error", "이미지 크기는 2MB를 초과할 수 없습니다.");
            return CARD;
        }
        if (file.getContentType() == null || !ALLOWED.contains(file.getContentType().toLowerCase())) {
            model.addAttribute("error", "이미지를 처리할 수 없습니다. JPEG 또는 PNG 파일인지 확인해 주세요.");
            return CARD;
        }
        try {
            byte[] input = file.getBytes();
            byte[] webp100 = imageService.toWebpCover(input, 100, 100, ImageService.PROFILE_WEBP_QUALITY);
            byte[] webp400 = imageService.toWebpCover(input, 400, 400, ImageService.PROFILE_WEBP_QUALITY);

            String filename = "user_" + principal.getId() + "_" + (System.currentTimeMillis() / 1000) + ".webp";
            storage.putPublic(KEY_100 + filename, webp100, "image/webp");
            storage.putPublic(KEY_400 + filename, webp400, "image/webp");

            User user = userService.findById(principal.getId());
            String previous = user.getProfileImage();
            userService.updateProfileImage(user.getId(), filename);
            if (previous != null) {
                storage.deletePublic(KEY_100 + previous);
                storage.deletePublic(KEY_400 + previous);
            }
            authSession.refresh(userService.findById(user.getId()), request, response);
            model.addAttribute("success", "프로필 이미지가 업데이트되었습니다.");
        } catch (Exception e) {
            model.addAttribute("error", "이미지를 처리할 수 없습니다. JPEG 또는 PNG 파일인지 확인해 주세요.");
        }
        return CARD;
    }

    @PostMapping("/client/profile/delete")
    public String delete(@AuthenticationPrincipal SecurityPrincipal principal,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        User user = userService.findById(principal.getId());
        if (user.getProfileImage() != null) {
            storage.deletePublic(KEY_100 + user.getProfileImage());
            storage.deletePublic(KEY_400 + user.getProfileImage());
            userService.updateProfileImage(user.getId(), null);
            authSession.refresh(userService.findById(user.getId()), request, response);
            model.addAttribute("success", "프로필 이미지가 삭제되었습니다.");
        } else {
            model.addAttribute("error", "삭제할 프로필 이미지가 없습니다.");
        }
        return CARD;
    }
}
