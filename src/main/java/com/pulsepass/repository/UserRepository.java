package com.pulsepass.repository;

import com.pulsepass.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    List<User> findByActiveTrueOrderByUsernameAsc();

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);
}
