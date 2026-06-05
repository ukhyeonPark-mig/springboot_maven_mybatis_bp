package com.example.bp.web.home;

import java.time.Duration;
import java.util.Locale;

import com.example.bp.domain.User;
import com.example.bp.security.AuthSessionService;
import com.example.bp.service.MailService;
import com.example.bp.service.OtpService;
import com.example.bp.service.TurnstileService;
import com.example.bp.service.UserService;
import com.example.bp.support.AppProperties;
import com.example.bp.support.FlashMessage;
import com.example.bp.support.PasswordPolicy;
import com.example.bp.support.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Unified auth page (FR-2): login / signup / OTP password-reset in one card,
 * with HTMX fragment swaps for tab switching and each action. Authentication is
 * performed manually so rate-limiting, Turnstile, and inline error fragments
 * behave like the Livewire reference.
 */
@Controller
public class SigninController {

    private static final String CARD = "auth/signin :: card";
    private static final String REDIRECT = "fragments/empty :: empty";
    private static final String OTP_VERIFIED_EMAIL = "otpVerifiedEmail";

    private static final String PAGE_SIGNIN = "Signin";
    private static final String PAGE_SIGNUP = "Signup";
    private static final String PAGE_FORGOT = "Forgot Password";

    private final TurnstileService turnstileService;
    private final RateLimiterService rateLimiter;
    private final OtpService otpService;
    private final UserService userService;
    private final AuthSessionService authSession;
    private final MailService mailService;
    private final SpringTemplateEngine templateEngine;
    private final boolean local;
    private final String appName;

    public SigninController(TurnstileService turnstileService, RateLimiterService rateLimiter,
                            OtpService otpService, UserService userService, AuthSessionService authSession,
                            MailService mailService, SpringTemplateEngine templateEngine,
                            Environment environment, AppProperties appProperties) {
        this.turnstileService = turnstileService;
        this.rateLimiter = rateLimiter;
        this.otpService = otpService;
        this.userService = userService;
        this.authSession = authSession;
        this.mailService = mailService;
        this.templateEngine = templateEngine;
        this.local = environment.matchesProfiles("local");
        this.appName = appProperties.name();
    }

    // ── Page + tab switching ─────────────────────────────────────────────────
    @GetMapping("/signin")
    public String signin(HttpSession session, Model model) {
        session.removeAttribute(OTP_VERIFIED_EMAIL);
        populate(model, PAGE_SIGNIN, false, false, null, null);
        return "auth/signin";
    }

    @GetMapping("/signin/tab")
    public String tab(@RequestParam("page") String page, HttpSession session, Model model) {
        session.removeAttribute(OTP_VERIFIED_EMAIL);
        populate(model, normalizePage(page), false, false, null, null);
        return CARD;
    }

    // ── Login (FR-2.1) ───────────────────────────────────────────────────────
    @PostMapping("/signin/login")
    public String login(@RequestParam(required = false) String email,
                        @RequestParam(required = false) String password,
                        @RequestParam(required = false) String turnstileToken,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        String ip = request.getRemoteAddr();
        String key = rateLimiter.key("signin", ip);

        RateLimiterService.Result limit = rateLimiter.attempt(key, 5, Duration.ofSeconds(60));
        if (!limit.allowed()) {
            return blocked(model, response, PAGE_SIGNIN, email, null, false, false, limit.retryAfterSeconds());
        }
        if (!turnstileService.verify(turnstileToken, ip)) {
            return turnstileFailed(model, response, PAGE_SIGNIN, email, null, false, false);
        }
        try {
            Authentication auth = authSession.authenticate(email, password);
            String target = authSession.establishAndResolveTarget(auth, request, response);
            rateLimiter.reset(key);
            response.setHeader("HX-Redirect", target);
            return REDIRECT;
        } catch (AuthenticationException e) {
            response.setHeader("HX-Trigger", "turnstileReset");
            model.addAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            populate(model, PAGE_SIGNIN, false, false, email, null);
            return CARD;
        }
    }

    // ── Signup (FR-2.2) ──────────────────────────────────────────────────────
    @PostMapping("/signin/signup")
    public String signup(@RequestParam(required = false) String email,
                         @RequestParam(required = false) String name,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) String turnstileToken,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        // Validation (reference order: validate -> rate limit -> turnstile -> create)
        String validationError = validateSignup(email, name, password);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            populate(model, PAGE_SIGNUP, false, false, email, name);
            return CARD;
        }

        String ip = request.getRemoteAddr();
        String key = rateLimiter.key("signup", ip);
        RateLimiterService.Result limit = rateLimiter.attempt(key, 3, Duration.ofSeconds(300));
        if (!limit.allowed()) {
            return blocked(model, response, PAGE_SIGNUP, email, name, false, false, limit.retryAfterSeconds());
        }
        if (!turnstileService.verify(turnstileToken, ip)) {
            return turnstileFailed(model, response, PAGE_SIGNUP, email, name, false, false);
        }

        User user = userService.create(email, name, password, "client", false);
        Authentication auth = authSession.tokenFor(user);
        String target = authSession.establishAndResolveTarget(auth, request, response);
        response.setHeader("HX-Redirect", target);
        return REDIRECT;
    }

    // ── Password reset: send code (FR-2.3 step 1) ────────────────────────────
    @PostMapping("/signin/reset/send")
    public String resetSend(@RequestParam(required = false) String email,
                           HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!StringUtils.hasText(email)) {
            model.addAttribute("error", "이메일을 입력해 주세요.");
            populate(model, PAGE_FORGOT, false, false, email, null);
            return CARD;
        }
        String key = rateLimiter.key("reset-send", request.getRemoteAddr());
        RateLimiterService.Result limit = rateLimiter.attempt(key, 3, Duration.ofSeconds(300));
        if (!limit.allowed()) {
            return blocked(model, response, PAGE_FORGOT, email, null, false, false, limit.retryAfterSeconds());
        }

        User user = userService.findByEmail(email);
        if (user == null) {
            model.addAttribute("error", "이메일을 찾을 수 없습니다.");
            populate(model, PAGE_FORGOT, false, false, email, null);
            return CARD;
        }

        String otp = otpService.generate();
        userService.setOtp(user.getId(), otp, otpService.expiryFromNow());
        try {
            mailService.sendHtml(user.getEmail(), user.getName(), "비밀번호 재설정 코드", renderOtpEmail(otp));
            model.addAttribute("success", "비밀번호 재설정 코드를 이메일로 전송했습니다. 10분 이내에 입력해 주세요.");
            populate(model, PAGE_FORGOT, true, false, email, null);
        } catch (Exception e) {
            model.addAttribute("error", "메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            populate(model, PAGE_FORGOT, false, false, email, null);
        }
        return CARD;
    }

    // ── Password reset: verify code (FR-2.3 step 2) ──────────────────────────
    @PostMapping("/signin/reset/verify")
    public String resetVerify(@RequestParam(required = false) String email,
                             @RequestParam(required = false) String code,
                             HttpServletRequest request, HttpSession session, Model model) {
        if (!StringUtils.hasText(code) || !code.chars().allMatch(Character::isDigit)) {
            model.addAttribute("error", "인증 코드를 올바르게 입력해 주세요.");
            populate(model, PAGE_FORGOT, true, false, email, null);
            return CARD;
        }
        String key = rateLimiter.key("reset-verify", request.getRemoteAddr());
        RateLimiterService.Result limit = rateLimiter.attempt(key, 10, Duration.ofSeconds(600));
        if (!limit.allowed()) {
            model.addAttribute("error", "요청이 너무 많습니다. " + limit.retryAfterSeconds() + "초 후 다시 시도해 주세요.");
            populate(model, PAGE_FORGOT, true, false, email, null);
            return CARD;
        }

        User user = userService.findByEmail(email);
        if (user == null || !StringUtils.hasText(user.getOtp()) || user.getOtpExpiresAt() == null) {
            model.addAttribute("error", "유효하지 않거나 만료된 코드입니다.");
            populate(model, PAGE_FORGOT, true, false, email, null);
            return CARD;
        }
        if (otpService.isExpired(user.getOtpExpiresAt())) {
            userService.invalidateOtp(user.getId());
            model.addAttribute("error", "인증 코드가 만료되었습니다. 다시 요청해 주세요.");
            populate(model, PAGE_FORGOT, true, false, email, null);
            return CARD;
        }
        if (otpService.attemptsExceeded(user.getOtpAttempts())) {
            userService.invalidateOtp(user.getId());
            model.addAttribute("error", "시도 횟수를 초과했습니다. 인증 코드를 다시 요청해 주세요.");
            populate(model, PAGE_FORGOT, true, false, email, null);
            return CARD;
        }
        if (!otpService.matches(user.getOtp(), code)) {
            userService.incrementOtpAttempts(user.getId());
            int remaining = otpService.remainingAttempts(user.getOtpAttempts() + 1);
            model.addAttribute("error", "유효하지 않은 코드입니다. (남은 시도: " + remaining + "회)");
            populate(model, PAGE_FORGOT, true, false, email, null);
            return CARD;
        }

        session.setAttribute(OTP_VERIFIED_EMAIL, email);
        model.addAttribute("success", "코드가 확인되었습니다. 새 비밀번호를 설정해 주세요.");
        populate(model, PAGE_FORGOT, true, true, email, null);
        return CARD;
    }

    // ── Password reset: set new password (FR-2.3 step 3) ─────────────────────
    @PostMapping("/signin/reset/password")
    public String resetPassword(@RequestParam(required = false) String email,
                               @RequestParam(required = false) String password,
                               HttpServletRequest request, HttpServletResponse response,
                               HttpSession session, Model model) {
        boolean verified = email != null && email.equals(session.getAttribute(OTP_VERIFIED_EMAIL));

        if (!PasswordPolicy.isValid(password)) {
            model.addAttribute("error", PasswordPolicy.MESSAGE);
            populate(model, PAGE_FORGOT, true, verified, email, null);
            return CARD;
        }

        User user = userService.findByEmail(email);
        if (user != null && verified) {
            userService.resetPassword(user.getId(), password);
            session.removeAttribute(OTP_VERIFIED_EMAIL);
            User fresh = userService.findByEmail(email);
            Authentication auth = authSession.tokenFor(fresh);
            String target = authSession.establishAndResolveTarget(auth, request, response);
            FlashMessage.success(session, "비밀번호가 성공적으로 재설정되었습니다.");
            response.setHeader("HX-Redirect", target);
            return REDIRECT;
        }

        model.addAttribute("error", "비밀번호 재설정에 실패했습니다.");
        populate(model, PAGE_FORGOT, true, verified, email, null);
        return CARD;
    }

    // ── Local quick login (FR-2.4 / §6.4) ────────────────────────────────────
    @PostMapping("/signin/quick")
    public String quickLogin(@RequestParam("role") String role,
                            HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!local) {
            populate(model, PAGE_SIGNIN, false, false, null, null);
            return CARD;
        }
        User user = userService.findFirstByRole(role);
        if (user != null) {
            Authentication auth = authSession.tokenFor(user);
            String target = authSession.establishAndResolveTarget(auth, request, response);
            response.setHeader("HX-Redirect", target);
            return REDIRECT;
        }
        model.addAttribute("error", "해당 역할의 사용자를 찾을 수 없습니다.");
        populate(model, PAGE_SIGNIN, false, false, null, null);
        return CARD;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private String validateSignup(String email, String name, String password) {
        if (!StringUtils.hasText(email)) {
            return "이메일을 입력해 주세요.";
        }
        if (userService.existsByEmail(email)) {
            return "이미 사용 중인 이메일입니다.";
        }
        if (!PasswordPolicy.isValid(password)) {
            return PasswordPolicy.MESSAGE;
        }
        if (!StringUtils.hasText(name)) {
            return "이름을 입력해 주세요.";
        }
        return null;
    }

    private String blocked(Model model, HttpServletResponse response, String page, String email, String name,
                           boolean resetField, boolean codeMatch, long seconds) {
        response.setHeader("HX-Trigger", "turnstileReset");
        model.addAttribute("error", "요청이 너무 많습니다. " + seconds + "초 후 다시 시도해 주세요.");
        populate(model, page, resetField, codeMatch, email, name);
        return CARD;
    }

    private String turnstileFailed(Model model, HttpServletResponse response, String page, String email, String name,
                                   boolean resetField, boolean codeMatch) {
        response.setHeader("HX-Trigger", "turnstileReset");
        model.addAttribute("error", "스팸 방지 검증에 실패했습니다. 다시 시도해 주세요.");
        populate(model, page, resetField, codeMatch, email, name);
        return CARD;
    }

    private void populate(Model model, String page, boolean resetField, boolean codeMatch, String email, String name) {
        model.addAttribute("page", page);
        model.addAttribute("resetField", resetField);
        model.addAttribute("codeMatch", codeMatch);
        model.addAttribute("email", email);
        model.addAttribute("name", name);
        model.addAttribute("turnstileEnabled", turnstileService.enabled());
        model.addAttribute("turnstileSiteKey", turnstileService.siteKey());
    }

    private String normalizePage(String page) {
        if (PAGE_SIGNUP.equals(page)) {
            return PAGE_SIGNUP;
        }
        if (PAGE_FORGOT.equals(page)) {
            return PAGE_FORGOT;
        }
        return PAGE_SIGNIN;
    }

    private String renderOtpEmail(String otp) {
        Context context = new Context(Locale.KOREAN);
        context.setVariable("otp", otp);
        context.setVariable("appName", appName);
        return templateEngine.process("emails/password_reset", context);
    }
}
