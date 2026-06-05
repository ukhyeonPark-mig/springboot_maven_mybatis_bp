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
 * HTML mail via AWS SES SMTP (PRD §4.2). Used for OTP reset mails and contact
 * submissions (with optional reply-to + attachment).
 *
 * <p>When no {@link JavaMailSender} is configured (e.g. local without SMTP),
 * the message is logged instead of sent — mirroring the reference's
 * {@code MAIL_MAILER=log} local behaviour, so dev flows don't break.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    /** Optional mail attachment (contact form, PRD §FR-3). */
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

    /** Thrown when sending fails; callers surface the Korean error toast. */
    public static class MailSendException extends RuntimeException {
        public MailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
