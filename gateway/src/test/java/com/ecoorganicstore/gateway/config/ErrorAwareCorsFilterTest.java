package com.ecoorganicstore.gateway.config;

import jakarta.servlet.DispatcherType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorAwareCorsFilterTest {

    @Test
    void addsAllowOriginWhenTheGatewayFailsTheRequest() throws Exception {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://www.eco-organic-store.com"));
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/checkout/sessions");
        request.addHeader(HttpHeaders.ORIGIN, "https://www.eco-organic-store.com");
        request.setDispatcherType(DispatcherType.ERROR);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(504);

        new ErrorAwareCorsFilter(source).doFilter(request, response, (req, res) -> { });

        assertEquals("https://www.eco-organic-store.com", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
