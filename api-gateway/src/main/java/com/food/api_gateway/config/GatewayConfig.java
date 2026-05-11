package com.food.api_gateway.config;

import com.food.api_gateway.filter.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GatewayConfig {

    @Bean
    public HandlerFilterFunction<ServerResponse, ServerResponse> authenticationFilterFunction(AuthenticationFilter filter) {
        return filter.filter();
    }
}
