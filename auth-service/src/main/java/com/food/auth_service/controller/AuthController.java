package com.food.auth_service.controller;

import com.food.auth_service.Entity.BlackListedToken;
import com.food.auth_service.Entity.User;
import com.food.auth_service.Entity.User.UserRole;
import com.food.auth_service.config.JwtProvider;
import com.food.auth_service.dto.AuthResponse;
import com.food.auth_service.dto.LoginRequest;
import com.food.auth_service.dto.RegisterRequest;
import com.food.auth_service.repository.BlackListedTokenRepository;
import com.food.auth_service.repository.UserRepository;
import com.food.auth_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION_MINUTES = 15;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private BlackListedTokenRepository blacklistedTokenRepository;

    @Autowired
    private KafkaTemplate<String, Long> kafkaTemplate;

    @PostMapping("/signup")
    public ResponseEntity<Object> registerUser(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already used with another account");
        }

        User createdUser = new User();
        createdUser.setEmail(req.getEmail());
        createdUser.setFullName(req.getFullName());
        createdUser.setRole(req.getRole() != null ? req.getRole() : UserRole.ROLE_CUSTOMER);
        createdUser.setPassword(passwordEncoder.encode(req.getPassword()));

        User savedUser = userRepository.save(createdUser);

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(savedUser.getRole().toString()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(savedUser.getEmail(), null,
                authorities);

        String jwt = jwtProvider.generateToken(authentication, savedUser.getId(), savedUser.getFullName());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Register Success");
        authResponse.setRole(savedUser.getRole());

        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    private Authentication authenticate(String emailId, String password) {
        UserDetails userDetails = userService.loadUserByUsername(emailId); // userservice implements
                                                                           // userdetailsservice (interface) from
                                                                           // spring security where it uses
                                                                           // loadbyusername()(default)
                                                                           // so inside i have simply passed the
                                                                           // emailId, which i have used for
                                                                           // authenticating the users.
        if (userDetails == null || !passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @PostMapping("/signin")
    @Transactional
    public ResponseEntity<Object> loginUser(@Valid @RequestBody LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid User"));
        if (!user.isAccountNonLocked()) {
            if (user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body("Account locked due to too many failed attempts. Try again later...");
            } else {
                // Lock expired, unlock account
                user.setAccountNonLocked(true);
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
            }
        }

        try {
            Authentication authentication = authenticate(loginRequest.getEmail(), loginRequest.getPassword());
            String jwt = jwtProvider.generateToken(authentication, user.getId(), user.getFullName());

            // If the user logs in correctly, we reset their failed attempts to zero.
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            UserRole role = UserRole.ROLE_CUSTOMER;
            if (!authentication.getAuthorities().isEmpty()) {
                role = UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority());
            }

            AuthResponse authResponse = new AuthResponse();
            authResponse.setJwt(jwt);
            authResponse.setMessage("Login Success");
            authResponse.setRole(role);

            return new ResponseEntity<>(authResponse, HttpStatus.OK);

        } catch (BadCredentialsException e) {
            // If they get the password wrong, we count it. After several failures, we lock
            // them out for security.
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountNonLocked(false);
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
            }
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    // When a user logs out, we put their session key on a "blocked list" so it
    // can't be used again.
    @PostMapping("/logout")
    public ResponseEntity<Object> logoutUser(@RequestHeader("Authorization") String jwt) {
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }

        // We save the logged-out key in the database so the system knows it is no
        // longer valid.
        if (!blacklistedTokenRepository.findByToken(jwt).isPresent()) {
            BlackListedToken bt = new BlackListedToken();
            bt.setToken(jwt);
            bt.setExpiryDate(jwtProvider.getExpirationDateFromToken(jwt));
            blacklistedTokenRepository.save(bt);
        }

        return ResponseEntity.ok("Successfully logged out (token revoked)");
    }

    @GetMapping("/validateToken")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        boolean isBlacklisted = blacklistedTokenRepository.findByToken(token).isPresent();
        return ResponseEntity.ok(isBlacklisted);
    }

    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        userRepository.deleteById(userId);
        
        // Notify other services that this user has been deleted so they can clean up their data
        kafkaTemplate.send("delete-user-topic", Long.valueOf(userId.toString()));
        
        return ResponseEntity.noContent().build();
    }
}
