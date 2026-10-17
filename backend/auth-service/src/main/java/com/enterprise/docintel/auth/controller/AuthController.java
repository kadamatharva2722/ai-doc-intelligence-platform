package com.enterprise.docintel.auth.controller;

import com.enterprise.docintel.auth.entity.User;
import com.enterprise.docintel.auth.repository.UserRepository;
import com.enterprise.docintel.auth.security.JwtTokenProvider;
import com.enterprise.docintel.common.dto.AuthResponse;
import com.enterprise.docintel.common.dto.LoginRequest;
import com.enterprise.docintel.common.dto.RegisterRequest;
import com.enterprise.docintel.common.entity.Role;
import com.enterprise.docintel.common.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        String username = registerRequest.getUsername() != null ? registerRequest.getUsername().trim() : "";
        String email = registerRequest.getEmail() != null ? registerRequest.getEmail().trim().toLowerCase() : "";

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken!");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email Address already in use!");
        }

        Role role;
        try {
            role = Role.valueOf("ROLE_" + registerRequest.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid Role: " + registerRequest.getRole());
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername() != null ? loginRequest.getUsername().trim() : "";

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User details not found post-authentication"));

        return ResponseEntity.ok(new AuthResponse(jwt, user.getUsername(), user.getRole().name()));
    }
}
