package com.ticketing.gateway.config;

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class FallbackRouteConfig {

    @Bean
    public RouterFunction<ServerResponse> fallbackRoute() {
        return GatewayRouterFunctions.route("fallbackRoute")
                .POST("/fallbackRoute", request -> ServerResponse
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Booking service is down"))
                .build();
    }
}