package com.example.bp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.example.bp.support.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * HTML 메일 발송 (PRD §4.2). OTP 재설정 메일과 문의 제출(선택적 reply-to + 첨부파일 포함)에 쓰인다.
 *
 * 발송 경로는 설정에 따라 자동으로 선택된다:
 *   - app.ses 키가 있으면  → AWS SES API(SDK)로 발송 (IAM 액세스 키/시크릿을 그대로 사용)
 *   - 아니면 JavaMailSender(spring.mail SMTP)가 있으면 → SMTP로 발송
 *   - 둘 다 없으면          → 발송 대신 로그로 남김 (SMTP 없는 local 개발 편의)
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    /** 선택적 메일 첨부파일 (문의 폼, PRD §FR-3). */
    public record Attachment(String filename, byte[] bytes, String contentType) {
    }

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties.Mail mailConfig;
    private final AppProperties.Ses sesConfig;
    private final boolean sesEnabled;

    private volatile SesV2Client sesClient;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider, AppProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailConfig = properties.mail();
        this.sesConfig = properties.ses();
        this.sesEnabled = sesConfig != null
                && StringUtils.hasText(sesConfig.accessKeyId())
                && StringUtils.hasText(sesConfig.secretAccessKey())
                && StringUtils.hasText(sesConfig.region());
    }

    public void sendHtml(String to, String toName, String subject, String html) {
        sendHtml(to, toName, subject, html, null, null);
    }

    public void sendHtml(String to, String toName, String subject, String html,
                         String replyTo, Attachment attachment) {
        if (sesEnabled) {
            sendViaSes(to, subject, html, replyTo, attachment);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("[MAIL:log] to={} <{}> subject=\"{}\"\n{}", toName, to, subject, html);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            fill(message, to, subject, html, replyTo, attachment);
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("Failed to send mail to " + to, e);
        }
    }

    /** SES API(SDK)로 발송: MIME 메시지를 raw 바이트로 만들어 SendEmail에 전달한다. */
    private void sendViaSes(String to, String subject, String html,
                            String replyTo, Attachment attachment) {
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
            fill(message, to, subject, html, replyTo, attachment);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            message.writeTo(out);
            ses().sendEmail(SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(to).build())
                    .content(EmailContent.builder()
                            .raw(RawMessage.builder().data(SdkBytes.fromByteArray(out.toByteArray())).build())
                            .build())
                    .build());
        } catch (MessagingException | IOException e) {
            throw new MailSendException("Failed to send mail to " + to, e);
        }
    }

    /** from/to/subject/html(+reply-to, 첨부)을 MIME 메시지에 채운다 (SMTP·SES 공통). */
    private void fill(MimeMessage message, String to, String subject, String html,
                      String replyTo, Attachment attachment)
            throws MessagingException, UnsupportedEncodingException {
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
    }

    /** SES 클라이언트 (최초 사용 시 생성, 지연 초기화). */
    private SesV2Client ses() {
        if (sesClient == null) {
            synchronized (this) {
                if (sesClient == null) {
                    sesClient = SesV2Client.builder()
                            .region(Region.of(sesConfig.region()))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(sesConfig.accessKeyId(), sesConfig.secretAccessKey())))
                            .build();
                }
            }
        }
        return sesClient;
    }

    /** 발송 실패 시 던져진다. 호출자는 한국어 오류 토스트를 노출한다. */
    public static class MailSendException extends RuntimeException {
        public MailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
