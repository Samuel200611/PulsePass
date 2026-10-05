package com.pulsepass.mapper;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.response.UserResponse;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-05T16:33:14-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        String firstName = null;
        String lastName = null;
        String phone = null;
        String city = null;
        LocalDate birthDate = null;
        Long id = null;
        String username = null;
        String email = null;
        boolean active = false;

        firstName = userProfileFirstName( user );
        lastName = userProfileLastName( user );
        phone = userProfilePhone( user );
        city = userProfileCity( user );
        birthDate = userProfileBirthDate( user );
        id = user.getId();
        username = user.getUsername();
        email = user.getEmail();
        active = user.isActive();

        UserResponse userResponse = new UserResponse( id, username, email, active, firstName, lastName, phone, city, birthDate );

        return userResponse;
    }

    private String userProfileFirstName(User user) {
        UserProfile profile = user.getProfile();
        if ( profile == null ) {
            return null;
        }
        return profile.getFirstName();
    }

    private String userProfileLastName(User user) {
        UserProfile profile = user.getProfile();
        if ( profile == null ) {
            return null;
        }
        return profile.getLastName();
    }

    private String userProfilePhone(User user) {
        UserProfile profile = user.getProfile();
        if ( profile == null ) {
            return null;
        }
        return profile.getPhone();
    }

    private String userProfileCity(User user) {
        UserProfile profile = user.getProfile();
        if ( profile == null ) {
            return null;
        }
        return profile.getCity();
    }

    private LocalDate userProfileBirthDate(User user) {
        UserProfile profile = user.getProfile();
        if ( profile == null ) {
            return null;
        }
        return profile.getBirthDate();
    }
}
