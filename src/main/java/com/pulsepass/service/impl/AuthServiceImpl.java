package com.pulsepass.service.impl;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    private final UserRepository userRepository;

    private final UserCredentialRepository userCredentialRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public AuthServiceImpl(UserService userService,
                           UserRepository userRepository,
                           UserCredentialRepository userCredentialRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {

        UserResponse response = userService.register(request);

        User user = userRepository.findById(response.id())
                .orElseThrow(() -> new IllegalStateException(
                        "User was just created but could not be found: " + response.id()));

        String passwordHash = passwordEncoder.encode(request.password());
        userCredentialRepository.save(new UserCredential(user, passwordHash));

        user.addRole(Role.USER);
        userRepository.save(user);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticate(request);

        SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(principal.getUserId());

        return new AuthResponse(accessToken, "Bearer", jwtService.getAccessTokenTtlSeconds());
    }

    private Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.identifier(), request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException(
                    "Invalid username/email or password.");
        }
    }

    @Override
    public AuthenticatedUserResponse getAuthenticatedUser(SecurityPrincipal principal) {
        List<String> roles = principal.getRoles().stream()
                .map(Enum::name)
                .toList();

        return new AuthenticatedUserResponse(
                principal.getUserId(),
                principal.getUsername(),
                principal.getEmail(),
                roles);
    }
}
