package com.ecoorganicstore.identity.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountMailer implements AccountMail {
    private static final Logger log = LoggerFactory.getLogger(AccountMailer.class);

    private final MailDispatcher dispatcher;
    private final StoreMessages messages;

    public AccountMailer(MailDispatcher dispatcher, StoreMessages messages) {
        this.dispatcher = dispatcher;
        this.messages = messages;
    }

    @Override
    public void welcome(String name, String email) {
        send(messages.welcome(name, email));
    }

    @Override
    public void passwordReset(String name, String email, String resetUrl) {
        send(messages.passwordReset(name, email, resetUrl));
    }

    @Override
    public void passwordChanged(String name, String email) {
        send(messages.passwordChanged(name, email));
    }

    @Override
    public void googleSignIn(String name, String email) {
        send(messages.googleSignIn(name, email));
    }

    private void send(OutboundMail mail) {
        try {
            dispatcher.dispatch(mail);
        } catch (RuntimeException ex) {
            log.warn("Account email '{}' was not queued for {}: {}", mail.subject(), mail.to(), ex.toString());
        }
    }
}
