package com.food.auth_service.scheduler;

import com.food.auth_service.repository.BlackListedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
public class CleanupScheduler {

    @Autowired
    private BlackListedTokenRepository blackListedTokenRepository;

    @Scheduled(fixedRate = 900000)
    @Transactional
    public void cleanExpiredToken() {
        blackListedTokenRepository.deleteByExpiryDateBefore(new Date());
    }
}
