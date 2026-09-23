package com.ecoorganicstore.gateway.config;

import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Security's CORS filter skips the error dispatcher, so a gateway timeout
 * reaches the browser without Access-Control-Allow-Origin. This filter also runs
 * on that dispatcher.
 */
public class ErrorAwareCorsFilter extends CorsFilter {

    public ErrorAwareCorsFilter(CorsConfigurationSource configSource) {
        super(configSource);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
