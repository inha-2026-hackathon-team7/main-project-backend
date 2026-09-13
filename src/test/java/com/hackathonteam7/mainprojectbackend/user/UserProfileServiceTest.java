package com.hackathonteam7.mainprojectbackend.user;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock UserRepository userRepository;
    UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(userRepository);
    }

    @Test
    void returnsOnlyPublicProfileFields() {
        User user = User.builder()
                .name("여행자")
                .email("traveler@example.com")
                .password("secret-hash")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var result = service.getProfile(42L);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.name()).isEqualTo("여행자");
        assertThat(result.email()).isEqualTo("traveler@example.com");
    }

    @Test
    void missingAuthenticatedUserReturnsNotFound() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(42L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
