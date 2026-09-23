package com.ecoorganicstore.identity.service.mail;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MailDispatcher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(MailDispatcher.class);

    private final MailPort mail;
    private final Executor executor;
    private final ExecutorService owned;

    public MailDispatcher(MailPort mail) {
        this.owned = Executors.newVirtualThreadPerTaskExecutor();
        this.executor = this.owned;
        this.mail = mail;
    }

    public MailDispatcher(MailPort mail, Executor executor) {
        this.mail = mail;
        this.executor = executor;
        this.owned = null;
    }

    public void dispatch(OutboundMail message) {
        executor.execute(() -> {
            try {
                mail.send(message);
            } catch (RuntimeException ex) {
                log.warn("Email '{}' to {} was not sent: {}", message.subject(), message.to(), ex.toString());
            }
        });
    }

    @Override
    public void close() {
        if (owned != null) {
            owned.close();
        }
    }
}
