package com.example.bp.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.example.bp.support.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AWS SES SMTP를 통한 HTML 메일 발송 (PRD §4.2). OTP 재설정 메일과 문의 제출
 * (선택적 reply-to + 첨부파일 포함)에 사용된다.
 *
 * <p>{@link JavaMailSender}가 설정되지 않은 경우(예: SMTP 없는 local), 메일을
 * 발송하는 대신 로그로 남긴다 — 원본의 {@code MAIL_MAILER=log} local 동작을
 * 그대로 따르므로 개발 흐름이 깨지지 않는다.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    /** 선택적 메일 첨부파일 (문의 폼, PRD §FR-3). */
    public record Attachment(String filename, byte[] bytes, String contentType) {
    }

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties.Mail mailConfig;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider, AppProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailConfig = properties.mail();
    }

    public void sendHtml(String to, String toName, String subject, String html) {
        sendHtml(to, toName, subject, html, null, null);
    }

    public void sendHtml(String to, String toName, String subject, String html,
                         String replyTo, Attachment attachment) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("[MAIL:log] to={} <{}> subject=\"{}\"\n{}", toName, to, subject, html);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, attachment != null, StandardCharsets.UTF_8.name());
            helper.setFrom(mailConfig.fromAddress(), mailConfig.fromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (StringUtils.hasText(replyTo)) {
                helper.setReplyTo(replyTo);
            }
            if (attachment != null) {
                helper.addAttachment(attachment.filename(),
                        new ByteArrayResource(attachment.bytes()), attachment.contentType());
            }
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("Failed to send mail to " + to, e);
        }
    }

    /** 발송 실패 시 던져진다. 호출자는 한국어 오류 토스트를 노출한다. */
    public static class MailSendException extends RuntimeException {
        public MailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
