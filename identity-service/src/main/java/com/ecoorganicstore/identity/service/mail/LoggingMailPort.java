package com.ecoorganicstore.identity.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingMailPort implements MailPort {
    private static final Logger log = LoggerFactory.getLogger(LoggingMailPort.class);

    @Override
    public void send(OutboundMail mail) {
        log.info("Email kept in the log (SMTP is off). to={} bcc={} subject={}\n{}",
                mail.to(), mail.bcc(), mail.subject(), mail.text());
    }
}
