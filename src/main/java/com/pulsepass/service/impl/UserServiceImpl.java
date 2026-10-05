package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * FR-SVC-010..012.
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    private final UserMapper mapper;

    public UserServiceImpl(UserRepository userRepository,
                           UserProfileRepository userProfileRepository,
                           UserMapper mapper) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.mapper = mapper;
    }

    // =====================================================
    // register — BR-USER-001..005
    // =====================================================

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {

        // BR-USER-001: username unico
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException(
                    "Username already exists: " + request.username());
        }

        // BR-USER-002: email unico ignorando mayusculas/minusculas
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException(
                    "Email already exists: " + request.email());
        }

        // BR-USER-005: la fecha de nacimiento no puede ser futura.
        // Se exige no nula porque TicketService la necesita mas adelante
        // para validar la edad minima de un evento (BR-TICKET-006).
        if (request.birthDate() == null) {
            throw new BusinessRuleException("Birth date is required.");
        }
        if (request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Birth date cannot be in the future: " + request.birthDate());
        }

        // BR-USER-003: todo usuario nuevo inicia activo
        User user = new User(request.username(), request.email(), true);

        UserProfile profile = new UserProfile(
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.city(),
                request.birthDate());

        // Mantiene sincronizados ambos lados del 1:1
        user.assignProfile(profile);

        // BR-USER-004: User y UserProfile se crean en la misma transaccion.
        // user_id es la FK en user_profiles, asi que el usuario debe
        // guardarse (y obtener su id) antes que el perfil.
        User savedUser = userRepository.save(user);
        userProfileRepository.save(profile);

        return mapper.toResponse(savedUser);
    }

    // =====================================================
    // Lecturas
    // =====================================================

    @Override
    public UserResponse findByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    @Override
    public UserResponse findByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + username));
    }
}
