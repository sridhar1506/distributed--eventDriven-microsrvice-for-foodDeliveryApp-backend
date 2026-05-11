package com.food.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.stream.Collectors;

import io.jsonwebtoken.io.Decoders;

// This filter acts like a security guard. It checks every request to make sure the user is allowed to be here.
@Component("AuthenticationFilter")
public class AuthenticationFilter {

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private final WebClient webClient;

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://AUTH-SERVICE").build();
    }

    public HandlerFilterFunction<ServerResponse, ServerResponse> filter() {
        return (request, next) -> {
            List<String> authHeaders = request.headers().header(HttpHeaders.AUTHORIZATION);
            
            // If the header is missing or empty, we stop them here.
            if (authHeaders.isEmpty() || !authHeaders.get(0).startsWith("Bearer ")) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
            }

            String token = authHeaders.get(0).substring(7);

            try {
                // We ask the Auth service if this specific token is still valid or if the user logged out.
                Boolean isBlacklisted = webClient.get()
                        .uri("/api/auth/validateToken?token=" + token)
                        .retrieve()
                        .bodyToMono(Boolean.class)
                        .block();

                if (Boolean.TRUE.equals(isBlacklisted)) {
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED).body("Token is logged out");
                }
            } catch (Exception e) {
                System.err.println("Could not reach the Security check service: " + e.getMessage());
                return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Auth service unreachable: " + e.getMessage());
            }

            try {
                // We decode the token to figure out who the user is and what they are allowed to do.
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = String.valueOf(claims.get("userId"));
                Object rolesObj = claims.get("authorities");
                String tempRoles = "";
                
                // We extract their roles (like "Owner" or "Customer").
                if (rolesObj instanceof List<?> list) {
                    tempRoles = list.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                } else if (rolesObj != null) {
                    tempRoles = rolesObj.toString();
                }

                final String roles = tempRoles;
                // We attach the User ID and Role to the request so the individual services know who is calling them.
                ServerRequest mutatedRequest = ServerRequest.from(request)
                        .headers(httpHeaders -> {
                            httpHeaders.remove("X-User-Id");
                            httpHeaders.remove("X-User-Role");
                            httpHeaders.remove("X-Internal-Key");

                            httpHeaders.add("X-User-Id", userId);
                        httpHeaders.add("X-User-Role", roles);
                            if (internalApiKey != null) {
                                httpHeaders.add("X-Internal-Key", internalApiKey);
                            }
                        })
                        .build();

                return next.handle(mutatedRequest);

            } catch (Exception e) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).body("Invalid login session: " + e.getMessage());
            }
        };
    }
}
