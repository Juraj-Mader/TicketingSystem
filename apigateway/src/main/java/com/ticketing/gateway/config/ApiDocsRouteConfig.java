package com.ticketing.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class ApiDocsRouteConfig {

    @Value("${BOOKING_SERVICE_URL:http://localhost:8081}")
    private String bookingServiceUrl;

    @Value("${INVENTORY_SERVICE_URL:http://localhost:8080}")
    private String inventoryServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> bookingServiceApiDocs() {
        return GatewayRouterFunctions.route("booking-service-api-docs")
                .route(path("/docs/bookingservice/v3/api-docs"), http())
                .before(uri(bookingServiceUrl))
                .filter(setPath("/v3/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceApiDocs() {
        return GatewayRouterFunctions.route("inventory-service-api-docs")
                .route(path("/docs/inventoryservice/v3/api-docs"), http())
                .before(uri(inventoryServiceUrl))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}