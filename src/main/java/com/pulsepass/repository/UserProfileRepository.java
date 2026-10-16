package com.pulsepass.repository;

import com.pulsepass.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUser_EmailIgnoreCase(String email);

    Optional<UserProfile> findByUser_Username(String username);
    List<UserProfile> findByCityIgnoreCaseOrderByLastNameAsc(String city);
}
