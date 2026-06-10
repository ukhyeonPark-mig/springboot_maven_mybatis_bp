package com.example.bp.web.admin;

import com.example.bp.domain.User;
import com.example.bp.security.AuthSessionService;
import com.example.bp.service.UserService;
import com.example.bp.support.PasswordPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 관리자 사용자 관리 (FR-9): 검색(숫자=id, 그 외 email/name LIKE), 역할
 * 필터, 페이지네이션(10), 인라인 생성/수정 카드, 삭제, 위장 로그인 — 모두
 * {@code #user-panel} 영역의 HTMX 프래그먼트 교체로 처리됩니다.
 */
@Controller
@RequestMapping("/admin/user")
public class AdminUserController {

    private static final String PANEL = "admin/user :: panel";
    private static final String EMAIL_CHECKED = "adminUserEmailChecked";

    private final UserService userService;
    private final AuthSessionService authSession;

    public AdminUserController(UserService userService, AuthSessionService authSession) {
        this.userService = userService;
        this.authSession = authSession;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "all") String role,
                       @RequestParam(defaultValue = "1") int page, Model model) {
        panel(model, search, role, page, "none");
        return "admin/user";
    }

    /** 검색 / 역할 필터 / 페이지네이션 / 생성 토글 — 프래그먼트 교체. */
    @GetMapping("/panel")
    public String panelFragment(@RequestParam(defaultValue = "") String search,
                               @RequestParam(defaultValue = "all") String role,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "none") String mode, Model model) {
        panel(model, search, role, page, mode);
        return PANEL;
    }

    @PostMapping("/check-email")
    public String checkEmail(@RequestParam(defaultValue = "") String email, HttpSession session, Model model) {
        boolean ok = StringUtils.hasText(email) && email.contains("@") && !userService.existsByEmail(email);
        if (ok) {
            session.setAttribute(EMAIL_CHECKED, email);
        } else {
            session.removeAttribute(EMAIL_CHECKED);
        }
        model.addAttribute("emailCheckedOk", ok);
        return "admin/user :: emailFeedback";
    }

    @PostMapping("/create")
    public String create(@RequestParam(defaultValue = "") String email,
                         @RequestParam(defaultValue = "") String name,
                         @RequestParam(defaultValue = "") String password,
                         @RequestParam(defaultValue = "client") String role,
                         @RequestParam(defaultValue = "") String search,
                         @RequestParam(defaultValue = "all") String filterRole,
                         @RequestParam(defaultValue = "1") int page,
                         HttpSession session, Model model) {
        boolean checked = email.equals(session.getAttribute(EMAIL_CHECKED));
        String error = null;
        if (!checked) {
            error = "이메일이 확인되지 않았거나 이미 사용 중입니다.";
        } else if (!StringUtils.hasText(name)) {
            error = "이름은 필수입니다.";
        } else if (!PasswordPolicy.isValid(password)) {
            error = PasswordPolicy.MESSAGE;
        }
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("createUserEmail", email);
            model.addAttribute("createUserName", name);
            model.addAttribute("createUserRole", role);
            model.addAttribute("emailCheckedOk", checked);
            panel(model, search, filterRole, page, "create");
            return PANEL;
        }
        userService.create(email, name, password, normalizeRole(role), true);
        session.removeAttribute(EMAIL_CHECKED);
        model.addAttribute("success", "계정이 성공적으로 생성되었습니다.");
        panel(model, search, filterRole, page, "none");
        return PANEL;
    }

    @GetMapping("/edit/{id}")
    public String editOpen(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String search,
                          @RequestParam(defaultValue = "all") String role,
                          @RequestParam(defaultValue = "1") int page, Model model) {
        User user = userService.findById(id);
        if (user != null) {
            model.addAttribute("editUserId", user.getId());
            model.addAttribute("editUserEmail", user.getEmail());
            model.addAttribute("editUserName", user.getName());
            model.addAttribute("editUserRole", user.getRole());
        }
        panel(model, search, role, page, "edit");
        return PANEL;
    }

    @PostMapping("/update")
    public String update(@RequestParam Long id,
                        @RequestParam(defaultValue = "") String name,
                        @RequestParam(defaultValue = "") String password,
                        @RequestParam(defaultValue = "client") String role,
                        @RequestParam(defaultValue = "") String search,
                        @RequestParam(defaultValue = "all") String filterRole,
                        @RequestParam(defaultValue = "1") int page, Model model) {
        if (!StringUtils.hasText(name)) {
            model.addAttribute("error", "이름은 필수입니다.");
            reopenEdit(model, id, role);
            panel(model, search, filterRole, page, "edit");
            return PANEL;
        }
        if (StringUtils.hasText(password) && !PasswordPolicy.isValid(password)) {
            model.addAttribute("error", PasswordPolicy.MESSAGE);
            reopenEdit(model, id, role, name);
            panel(model, search, filterRole, page, "edit");
            return PANEL;
        }
        userService.adminUpdate(id, name, normalizeRole(role), password);
        model.addAttribute("success", "계정이 성공적으로 업데이트되었습니다.");
        panel(model, search, filterRole, page, "none");
        return PANEL;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                        @RequestParam(defaultValue = "") String search,
                        @RequestParam(defaultValue = "all") String role,
                        @RequestParam(defaultValue = "1") int page, Model model) {
        userService.delete(id);
        model.addAttribute("success", "계정이 성공적으로 삭제되었습니다.");
        panel(model, search, role, page, "none");
        return PANEL;
    }

    @PostMapping("/impersonate/{id}")
    public String impersonate(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        User user = userService.findById(id);
        if (user != null) {
            authSession.refresh(user, request, response);
        }
        response.setHeader("HX-Redirect", request.getContextPath() + "/");
        return "fragments/empty :: empty";
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────
    private void panel(Model model, String search, String role, int page, String mode) {
        UserService.UserPage result = userService.searchPage(search, role, page);
        model.addAttribute("users", result.content());
        model.addAttribute("page", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("countAll", result.countAll());
        model.addAttribute("countClient", result.countClient());
        model.addAttribute("countAdmin", result.countAdmin());
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("mode", mode);
        model.addAttribute("r2PublicUrlSet", true);
    }

    private void reopenEdit(Model model, Long id, String role) {
        User user = userService.findById(id);
        model.addAttribute("editUserId", id);
        model.addAttribute("editUserEmail", user != null ? user.getEmail() : "");
        model.addAttribute("editUserName", user != null ? user.getName() : "");
        model.addAttribute("editUserRole", role);
    }

    private void reopenEdit(Model model, Long id, String role, String name) {
        User user = userService.findById(id);
        model.addAttribute("editUserId", id);
        model.addAttribute("editUserEmail", user != null ? user.getEmail() : "");
        model.addAttribute("editUserName", name);
        model.addAttribute("editUserRole", role);
    }

    private String normalizeRole(String role) {
        return "admin".equals(role) ? "admin" : "client";
    }
}
