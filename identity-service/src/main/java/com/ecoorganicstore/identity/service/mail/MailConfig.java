package com.ecoorganicstore.identity.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {
    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    StoreMessages storeMessages(
            @Value("${app.mail.from-name:Marni eco organic store}") String storeName,
            @Value("${app.mail.from:admin@eco-organic-store.com}") String fromAddress,
            @Value("${app.mail.admin:admin@eco-organic-store.com}") String adminAddress,
            @Value("${app.storefront-url:http://localhost:5173}") String storefrontUrl) {
        return new StoreMessages(storeName, fromAddress, adminAddress, storefrontUrl);
    }

    @Bean(destroyMethod = "close")
    MailDispatcher mailDispatcher(
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.host:}") String host,
            @Value("${app.mail.port:587}") int port,
            @Value("${app.mail.username:admin@eco-organic-store.com}") String username,
            @Value("${app.mail.password:}") String password,
            @Value("${app.mail.from:admin@eco-organic-store.com}") String fromAddress,
            @Value("${app.mail.from-name:Marni eco organic store}") String fromName) {
        boolean smtp = enabled && host != null && !host.isBlank() && password != null && !password.isBlank();
        MailPort portAdapter = smtp
                ? new SmtpMailPort(host.trim(), port, username, password, fromAddress, fromName)
                : new LoggingMailPort();
        if (!smtp) {
            log.info("Transactional email will be written to the log. Set MAIL_ENABLED, MAIL_HOST, and MAIL_PASSWORD to send from {}.", fromAddress);
        }
        return new MailDispatcher(portAdapter);
    }
}
