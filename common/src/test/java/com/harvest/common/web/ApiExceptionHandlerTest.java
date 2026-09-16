package com.harvest.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsUnauthorizedTo401() {
        HttpServletRequest request = mockRequest("/api/me");
        ProblemDetail pd = handler.unauthorized(new UnauthorizedException("Authentication required"), request);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), pd.getStatus());
        assertEquals("Unauthorized", pd.getTitle());
    }

    @Test
    void mapsForbiddenTo403() {
        HttpServletRequest request = mockRequest("/api/admin/orders");
        ProblemDetail pd = handler.forbidden(new ForbiddenException("Admin access required"), request);
        assertEquals(HttpStatus.FORBIDDEN.value(), pd.getStatus());
        assertEquals("Forbidden", pd.getTitle());
    }

    private HttpServletRequest mockRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
