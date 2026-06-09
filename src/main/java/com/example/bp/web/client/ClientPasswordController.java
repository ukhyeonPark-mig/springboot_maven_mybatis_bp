package com.example.bp.web.client;

import com.example.bp.domain.User;
import com.example.bp.security.SecurityPrincipal;
import com.example.bp.service.UserService;
import com.example.bp.support.PasswordPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 클라이언트 비밀번호 변경 (FR-6.2): 현재 비밀번호 확인 + 정책 검증 + 확인란 일치 검사. */
@Controller
public class ClientPasswordController {

    protected static final String CARD = "client/password :: card";
    protected static final String VIEW = "client/password";

    private final UserService userService;

    public ClientPasswordController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/client/password")
    public String password() {
        return VIEW;
    }

    @PostMapping("/client/password")
    public String update(@RequestParam(value = "current_password", required = false) String currentPassword,
                        @RequestParam(value = "new_password", required = false) String newPassword,
                        @RequestParam(value = "new_password_confirmation", required = false) String confirmation,
                        @AuthenticationPrincipal SecurityPrincipal principal, Model model) {
        User user = userService.findById(principal.getId());

        if (!StringUtils.hasText(currentPassword) || !userService.checkPassword(currentPassword, user.getPassword())) {
            model.addAttribute("error", "현재 비밀번호가 올바르지 않습니다.");
            return CARD;
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            model.addAttribute("error", PasswordPolicy.MESSAGE);
            return CARD;
        }
        if (!newPassword.equals(confirmation)) {
            model.addAttribute("error", "새 비밀번호가 일치하지 않습니다.");
            return CARD;
        }
        userService.changePassword(user.getId(), newPassword);
        model.addAttribute("success", "비밀번호가 성공적으로 업데이트되었습니다.");
        return CARD;
    }
}
