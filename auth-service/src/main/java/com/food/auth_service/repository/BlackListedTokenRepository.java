package com.food.auth_service.repository;

import com.food.auth_service.Entity.BlackListedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;

public interface BlackListedTokenRepository extends JpaRepository<BlackListedToken, Long> {

    Optional<BlackListedToken> findByToken(String token);
    void deleteByExpiryDateBefore(Date now);
}
