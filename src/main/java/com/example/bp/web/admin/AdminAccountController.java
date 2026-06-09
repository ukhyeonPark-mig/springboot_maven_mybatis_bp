package com.example.bp.web.admin;

import com.example.bp.domain.User;
import com.example.bp.security.AuthSessionService;
import com.example.bp.security.SecurityPrincipal;
import com.example.bp.service.ProfileImageService;
import com.example.bp.service.UserService;
import com.example.bp.support.PasswordPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** 관리자 계정 (FR-8): 프로필 이미지 + 비밀번호. 관리자 레이아웃에서 클라이언트 영역을 동일하게 반영합니다. */
@Controller
public class AdminAccountController {

    private static final String PROFILE_CARD = "admin/account/profile :: card";
    private static final String PASSWORD_CARD = "admin/account/password :: card";

    private final ProfileImageService profileImageService;
    private final UserService userService;
    private final AuthSessionService authSession;

    public AdminAccountController(ProfileImageService profileImageService, UserService userService,
                                  AuthSessionService authSession) {
        this.profileImageService = profileImageService;
        this.userService = userService;
        this.authSession = authSession;
    }

    @GetMapping("/admin/account/profile")
    public String profile() {
        return "admin/account/profile";
    }

    @PostMapping("/admin/account/profile")
    public String saveProfile(@RequestParam(value = "profile_image", required = false) MultipartFile file,
                             @AuthenticationPrincipal SecurityPrincipal principal,
                             HttpServletRequest request, HttpServletResponse response, Model model) {
        String error = profileImageService.validate(file);
        if (error != null) {
            model.addAttribute("error", error);
            return PROFILE_CARD;
        }
        try {
            profileImageService.replace(principal.getId(), file);
            authSession.refresh(userService.findById(principal.getId()), request, response);
            model.addAttribute("success", "프로필 이미지가 업데이트되었습니다.");
        } catch (Exception e) {
            model.addAttribute("error", "이미지를 처리할 수 없습니다. JPEG 또는 PNG 파일인지 확인해 주세요.");
        }
        return PROFILE_CARD;
    }

    @PostMapping("/admin/account/profile/delete")
    public String deleteProfile(@AuthenticationPrincipal SecurityPrincipal principal,
                               HttpServletRequest request, HttpServletResponse response, Model model) {
        if (profileImageService.remove(principal.getId())) {
            authSession.refresh(userService.findById(principal.getId()), request, response);
            model.addAttribute("success", "프로필 이미지가 삭제되었습니다.");
        } else {
            model.addAttribute("error", "삭제할 프로필 이미지가 없습니다.");
        }
        return PROFILE_CARD;
    }

    @GetMapping("/admin/account/password")
    public String password() {
        return "admin/account/password";
    }

    @PostMapping("/admin/account/password")
    public String updatePassword(@RequestParam(value = "current_password", required = false) String currentPassword,
                                @RequestParam(value = "new_password", required = false) String newPassword,
                                @RequestParam(value = "new_password_confirmation", required = false) String confirmation,
                                @AuthenticationPrincipal SecurityPrincipal principal, Model model) {
        User user = userService.findById(principal.getId());
        if (!StringUtils.hasText(currentPassword) || !userService.checkPassword(currentPassword, user.getPassword())) {
            model.addAttribute("error", "현재 비밀번호가 올바르지 않습니다.");
            return PASSWORD_CARD;
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            model.addAttribute("error", PasswordPolicy.MESSAGE);
            return PASSWORD_CARD;
        }
        if (!newPassword.equals(confirmation)) {
            model.addAttribute("error", "새 비밀번호가 일치하지 않습니다.");
            return PASSWORD_CARD;
        }
        userService.changePassword(user.getId(), newPassword);
        model.addAttribute("success", "비밀번호가 성공적으로 업데이트되었습니다.");
        return PASSWORD_CARD;
    }
}
