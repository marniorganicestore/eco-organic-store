package com.ecoorganicstore.identity.config;

import com.ecoorganicstore.identity.domain.PasswordResetToken;
import com.ecoorganicstore.identity.domain.SentNotice;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MailIndexConfig {
    public MailIndexConfig(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(PasswordResetToken.class)
                .createIndex(new Index().on("tokenHash", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps(PasswordResetToken.class)
                .createIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(0));
        mongoTemplate.indexOps(SentNotice.class)
                .createIndex(new Index().on("dedupeKey", Sort.Direction.ASC).unique());
    }
}
