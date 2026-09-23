package com.harvest.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class IdentityMongoConfig {
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(config -> config.registerConverter(new LegacyAddressReadConverter()));
    }
}
