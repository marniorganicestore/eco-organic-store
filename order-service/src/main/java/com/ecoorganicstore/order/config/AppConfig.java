package com.ecoorganicstore.order.config;

import com.ecoorganicstore.common.security.InternalKeyFilter;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;

@Configuration
@EnableWebSecurity
public class AppConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, InternalKeyFilter filter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean
    InternalKeyFilter internalKeyFilter(@Value("${app.internal-key}") String internalKey) {
        return new InternalKeyFilter(internalKey);
    }

    @Bean
    RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Longer than one Razorpay attempt (10s connect + 20s read) and shorter than the gateway read timeout.
        requestFactory.setReadTimeout(Duration.ofSeconds(40));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}