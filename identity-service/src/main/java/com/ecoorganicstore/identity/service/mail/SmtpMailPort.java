package com.ecoorganicstore.identity.service.mail;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

public final class SmtpMailPort implements MailPort {
    private final JavaMailSenderImpl sender;
    private final String fromAddress;
    private final String fromName;

    public SmtpMailPort(String host, int port, String username, String password, String fromAddress, String fromName) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        var properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.connectiontimeout", "8000");
        properties.put("mail.smtp.timeout", "8000");
        properties.put("mail.smtp.writetimeout", "8000");
        this.sender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(OutboundMail mail) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(mail.to());
            if (mail.bcc() != null && !mail.bcc().isBlank() && !mail.bcc().equalsIgnoreCase(mail.to())) {
                helper.addBcc(mail.bcc());
            }
            helper.setSubject(mail.subject());
            helper.setText(mail.text(), mail.html());
            sender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("SMTP send failed", ex);
        }
    }
}
