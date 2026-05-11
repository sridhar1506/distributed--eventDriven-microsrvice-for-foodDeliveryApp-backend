package com.food.auth_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtProvider {

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication auth, Long userId, String name) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        String roles = populateAuthorities(authorities);
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + 3600000)) // 1 hour expiration for each token
                .claim("email", auth.getName())
                .claim("authorities", roles)
                .claim("userId", userId)
                .claim("name", name)
                .signWith(key)
                .compact();
    }

    public Date getExpirationDateFromToken(String jwt) {
        if (jwt == null || jwt.trim().isEmpty()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (jwt.startsWith("Bearer "))
            jwt = jwt.substring(7);
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
        return claims.getExpiration();
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auths = new HashSet<>();
        for (GrantedAuthority authority : authorities) {
            auths.add(authority.getAuthority());
        }
        return String.join(",", auths);
    }

}
