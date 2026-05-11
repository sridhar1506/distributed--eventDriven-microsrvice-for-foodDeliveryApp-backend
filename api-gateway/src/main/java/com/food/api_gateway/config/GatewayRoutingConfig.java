package com.food.api_gateway.config;

import com.food.api_gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import org.springframework.http.HttpMethod;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
public class GatewayRoutingConfig {

        private final AuthenticationFilter authenticationFilter;

        public GatewayRoutingConfig(AuthenticationFilter authenticationFilter) {
                this.authenticationFilter = authenticationFilter;
        }

        @Bean
        public RouterFunction<ServerResponse> customRouteLocator() {
                return route("auth-service")
                                .route(RequestPredicates.GET("/api/auth/**"), HandlerFunctions.http())
                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                .lb("AUTH-SERVICE"))
                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("authCB",
                                                URI.create("forward:/fallback/auth")))
                                .build()
                                .and(route("auth-service-write")
                                                .route(RequestPredicates.method(HttpMethod.POST)
                                                                .or(RequestPredicates.method(HttpMethod.DELETE))
                                                                .and(RequestPredicates.path("/api/auth/**")),
                                                                HandlerFunctions.http())
                                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                                .lb("AUTH-SERVICE"))
                                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("authCBWrite",
                                                                URI.create("forward:/fallback/auth")))
                                                .build())
                                .and(route("restaurant-service-get")
                                                .route(RequestPredicates.GET("/api/restaurants/**")
                                                        .or(RequestPredicates.GET("/api/restaurants"))
                                                        .or(RequestPredicates.GET("/api/food/**"))
                                                        .or(RequestPredicates.GET("/api/food")),
                                                                HandlerFunctions.http())
                                                .filter(authenticationFilter.filter())
                                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                                .lb("RESTAURANT-SERVICE"))
                                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("restaurantCB",
                                                                URI.create("forward:/fallback/restaurant")))
                                                .build())
                                .and(route("restaurant-service-write")
                                                .route(RequestPredicates.method(HttpMethod.POST)
                                                                .or(RequestPredicates.method(HttpMethod.PUT))
                                                                .or(RequestPredicates.method(HttpMethod.DELETE))
                                                                .and(RequestPredicates.path("/api/restaurants/**")
                                                                                .or(RequestPredicates.path("/api/food/**"))),
                                                                HandlerFunctions.http())
                                                .filter(authenticationFilter.filter())
                                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                                .lb("RESTAURANT-SERVICE"))
                                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("restaurantCBWrite",
                                                                URI.create("forward:/fallback/restaurant")))
                                                .build())
                                .and(route("order-service-get")
                                                .route(RequestPredicates.GET("/api/orders/**")
                                                                .or(RequestPredicates.GET("/api/orders"))
                                                                .or(RequestPredicates.GET("/api/cart/**"))
                                                                .or(RequestPredicates.GET("/api/cart")),
                                                                HandlerFunctions.http())
                                                .filter(authenticationFilter.filter())
                                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                                .lb("ORDER-SERVICE"))
                                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("orderCB",
                                                                URI.create("forward:/fallback/order")))
                                                .build())
                                .and(route("order-service-write")
                                                .route(RequestPredicates.method(HttpMethod.POST)
                                                                .or(RequestPredicates.method(HttpMethod.PUT))
                                                                .or(RequestPredicates.method(HttpMethod.DELETE))
                                                                .and(RequestPredicates.path("/api/orders/**")
                                                                                .or(RequestPredicates.path("/api/orders"))
                                                                                .or(RequestPredicates.path("/api/cart/**"))
                                                                                .or(RequestPredicates.path("/api/cart"))),
                                                                HandlerFunctions.http())
                                                .filter(authenticationFilter.filter())
                                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                                .lb("ORDER-SERVICE"))
                                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("orderCBWrite",
                                                                URI.create("forward:/fallback/order")))
                                                .build())
                                .and(route("payment-service-get")
                                                .route(RequestPredicates.GET("/api/payments/**"),
                                                                HandlerFunctions.http())
                                                .filter(authenticationFilter.filter())
                                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                                .lb("PAYMENT-SERVICE"))
                                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("paymentCB",
                                                                URI.create("forward:/fallback/payment")))
                                                .build())
                                .and(route("payment-service-write")
                                                .route(RequestPredicates.POST("/api/payments/**")
                                                                .or(RequestPredicates.PUT("/api/payments/**"))
                                                                .or(RequestPredicates.DELETE("/api/payments/**")),
                                                                HandlerFunctions.http())
                                                .filter(authenticationFilter.filter())
                                                .filter(org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions
                                                                .lb("PAYMENT-SERVICE"))
                                                .filter(CircuitBreakerFilterFunctions.circuitBreaker("paymentCBWrite",
                                                                URI.create("forward:/fallback/payment")))
                                                .build());
        }
}
