package com.ecoorganicstore.identity.service.mail;

public record OutboundMail(String to, String bcc, String subject, String text, String html) {
}
