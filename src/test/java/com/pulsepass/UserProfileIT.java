package com.pulsepass;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-USR-001..004 (gestion de usuarios y perfiles), QT-004 (relacion
 * User 1:1 UserProfile) y QT-009 (restriccion UNIQUE con saveAndFlush).
 * Corresponde a la matriz de trazabilidad de la seccion 22 del PRD.
 */
@DisplayName("UserRepository / UserProfileRepository — FR-USR-001..004")
class UserProfileIT extends PersistenceTestSupport {

    @Test
    @DisplayName("FR-USR-004: el perfil se recupera navegando desde el usuario")
    void profileIsReachableFromUser() {
        User user = createUser("andrea", "andrea@pulsepass.co");
        UserProfile profile = new UserProfile("Andrea", "Gomez",
                "3001112233", "Santa Marta", LocalDate.of(1998, 5, 12));
        user.assignProfile(profile);
        userProfileRepository.saveAndFlush(profile);

        Optional<UserProfile> found =
                userProfileRepository.findByUser_EmailIgnoreCase("ANDREA@PULSEPASS.CO");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Andrea");
        assertThat(found.get().getUser().getUsername()).isEqualTo("andrea");
    }

    @Test
    @DisplayName("QT-004/AC-004: la BD impide un segundo perfil para el mismo usuario")
    void userCannotHaveTwoProfiles() {
        User user = createUser("carlos", "carlos@pulsepass.co");

        UserProfile first = new UserProfile("Carlos", "Diaz",
                null, "Barranquilla", LocalDate.of(1995, 1, 1));
        user.assignProfile(first);
        userProfileRepository.saveAndFlush(first);

        UserProfile duplicate = new UserProfile("Carlos", "Duplicado",
                null, "Barranquilla", LocalDate.of(1995, 1, 1));
        user.assignProfile(duplicate);

        // QT-009: user_id es UNIQUE; la violacion ocurre en PostgreSQL,
        // por eso se necesita saveAndFlush y no solo save().
        assertThrows(DataIntegrityViolationException.class,
                () -> userProfileRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("FR-USR-002: username y email son unicos")
    void userIdentityIsUnique() {
        createUser("laura", "laura@pulsepass.co");

        User duplicatedEmail = new User("otra-laura", "laura@pulsepass.co", true);

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicatedEmail));
    }

    @Test
    @DisplayName("FR-USR-001: el usuario puede persistirse y recuperarse")
    void userCanBePersistedAndRetrieved() {
        createUser("miguel", "miguel@pulsepass.co");

        Optional<User> found = userRepository.findByEmailIgnoreCase("MIGUEL@PULSEPASS.CO");

        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isTrue();
    }
}
