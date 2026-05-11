package com.food.auth_service.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


@Entity
@Getter
@Setter
@Table(name = "blacklisted_tokens")
public class BlackListedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false, unique = true)
    private String token;

    private Date expiryDate;

}
