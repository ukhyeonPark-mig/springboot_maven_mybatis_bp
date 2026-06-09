package com.example.bp.web.home;

import java.time.Duration;

import com.example.bp.service.MailService;
import com.example.bp.service.TurnstileService;
import com.example.bp.support.AppProperties;
import com.example.bp.support.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

/**
 * 공개 문의 폼 (FR-3): 발신자를 reply-to로 설정하고 선택적 첨부 파일과 함께
 * SES를 통해 제출 내용을 메일로 전송한다. IP당 10분에 3회로 rate-limit되며,
 * Turnstile로 보호된다. HTMX는 폼 fragment + 토스트를 반환한다.
 */
@Controller
public class ContactController {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024; // 10MB
    private static final int MAX_MESSAGE_LEN = 5000;
    private static final String CARD = "home/contact :: card";

    private final TurnstileService turnstileService;
    private final RateLimiterService rateLimiter;
    private final MailService mailService;
    private final AppProperties appProperties;

    public ContactController(TurnstileService turnstileService, RateLimiterService rateLimiter,
                             MailService mailService, AppProperties appProperties) {
        this.turnstileService = turnstileService;
        this.rateLimiter = rateLimiter;
        this.mailService = mailService;
        this.appProperties = appProperties;
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        populate(model, null, null, null, null);
        return "home/contact";
    }

    @PostMapping("/contact")
    public String send(@RequestParam(required = false) String name,
                      @RequestParam(required = false) String email,
                      @RequestParam(required = false) String subject,
                      @RequestParam(required = false) String message,
                      @RequestParam(required = false) MultipartFile file,
                      @RequestParam(required = false) String turnstileToken,
                      HttpServletRequest request, HttpServletResponse response, Model model) {

        String error = validate(name, email, subject, message, file);
        if (error != null) {
            model.addAttribute("error", error);
            populate(model, name, email, subject, message);
            return CARD;
        }

        String ip = request.getRemoteAddr();
        RateLimiterService.Result limit = rateLimiter.attempt("contact:" + ip, 3, Duration.ofSeconds(600));
        if (!limit.allowed()) {
            response.setHeader("HX-Trigger", "turnstileReset");
            model.addAttribute("error", "너무 많은 문의가 접수되었습니다. " + limit.retryAfterSeconds() + "초 후 다시 시도해 주세요.");
            populate(model, name, email, subject, message);
            return CARD;
        }
        if (!turnstileService.verify(turnstileToken, ip)) {
            response.setHeader("HX-Trigger", "turnstileReset");
            model.addAttribute("error", "스팸 방지 검증에 실패했습니다. 다시 시도해 주세요.");
            populate(model, name, email, subject, message);
            return CARD;
        }

        try {
            String toAddress = orElse(appProperties.contact().toAddress(), appProperties.mail().fromAddress());
            String toName = orElse(appProperties.contact().toName(), appProperties.name());
            MailService.Attachment attachment = toAttachment(file);
            mailService.sendHtml(toAddress, toName,
                    "[문의] " + subject + " — " + name,
                    buildHtml(name, email, subject, message),
                    email, attachment);
            model.addAttribute("success", "문의가 정상적으로 전송되었습니다. 빠른 시일 내에 답변 드리겠습니다.");
            populate(model, null, null, null, null); // 성공 시 초기화
        } catch (Exception e) {
            response.setHeader("HX-Trigger", "turnstileReset");
            model.addAttribute("error", "메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            populate(model, name, email, subject, message);
        }
        return CARD;
    }

    private String validate(String name, String email, String subject, String message, MultipartFile file) {
        if (!StringUtils.hasText(name) || name.length() > 255) {
            return "이름을 입력해 주세요.";
        }
        if (!StringUtils.hasText(email)) {
            return "이메일을 입력해 주세요.";
        }
        if (!StringUtils.hasText(subject) || subject.length() > 255) {
            return "제목을 입력해 주세요.";
        }
        if (!StringUtils.hasText(message) || message.length() > MAX_MESSAGE_LEN) {
            return "메시지를 5000자 이내로 입력해 주세요.";
        }
        if (file != null && !file.isEmpty() && file.getSize() > MAX_FILE_BYTES) {
            return "첨부 파일은 10MB를 초과할 수 없습니다.";
        }
        return null;
    }

    private String buildHtml(String name, String email, String subject, String message) {
        return "<h3>" + HtmlUtils.htmlEscape(appProperties.name()) + " — 신규 문의</h3>"
                + "<p><strong>이름:</strong> " + HtmlUtils.htmlEscape(name) + "<br>"
                + "<strong>이메일:</strong> " + HtmlUtils.htmlEscape(email) + "<br>"
                + "<strong>제목:</strong> " + HtmlUtils.htmlEscape(subject) + "</p>"
                + "<p><strong>메시지:</strong></p>"
                + "<p>" + HtmlUtils.htmlEscape(message).replace("\n", "<br>") + "</p>";
    }

    private MailService.Attachment toAttachment(MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return new MailService.Attachment(file.getOriginalFilename(), file.getBytes(), file.getContentType());
    }

    private void populate(Model model, String name, String email, String subject, String message) {
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("subject", subject);
        model.addAttribute("message", message);
        model.addAttribute("turnstileEnabled", turnstileService.enabled());
        model.addAttribute("turnstileSiteKey", turnstileService.siteKey());
    }

    private static String orElse(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
