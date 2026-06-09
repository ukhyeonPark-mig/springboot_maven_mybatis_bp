package com.example.bp.web.client;

import com.example.bp.security.AuthSessionService;
import com.example.bp.security.SecurityPrincipal;
import com.example.bp.service.ProfileImageService;
import com.example.bp.service.UserService;
import com.example.bp.web.exception.CardException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** 클라이언트 프로필 이미지 (FR-6.1) — 처리 파이프라인을 ProfileImageService에 위임한다. */
@Controller
public class ClientProfileController {

    private static final String CARD = "client/profile :: card";

    private final ProfileImageService profileImageService;
    private final UserService userService;
    private final AuthSessionService authSession;

    public ClientProfileController(ProfileImageService profileImageService, UserService userService,
                                   AuthSessionService authSession) {
        this.profileImageService = profileImageService;
        this.userService = userService;
        this.authSession = authSession;
    }

    @GetMapping("/client/profile")
    public String profile() {
        return "client/profile";
    }

    @PostMapping("/client/profile")
    public String save(@RequestParam(value = "profile_image", required = false) MultipartFile file,
                      @AuthenticationPrincipal SecurityPrincipal principal,
                      HttpServletRequest request, HttpServletResponse response, Model model) {
        String error = profileImageService.validate(file);
        if (error != null) {
            throw new CardException(CARD, error);
        }
        try {
            profileImageService.replace(principal.getId(), file);
            authSession.refresh(userService.findById(principal.getId()), request, response);
        } catch (Exception e) {
            throw new CardException(CARD, "이미지를 처리할 수 없습니다. JPEG 또는 PNG 파일인지 확인해 주세요.");
        }
        model.addAttribute("success", "프로필 이미지가 업데이트되었습니다.");
        return CARD;
    }

    @PostMapping("/client/profile/delete")
    public String delete(@AuthenticationPrincipal SecurityPrincipal principal,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!profileImageService.remove(principal.getId())) {
            throw new CardException(CARD, "삭제할 프로필 이미지가 없습니다.");
        }
        authSession.refresh(userService.findById(principal.getId()), request, response);
        model.addAttribute("success", "프로필 이미지가 삭제되었습니다.");
        return CARD;
    }
}
