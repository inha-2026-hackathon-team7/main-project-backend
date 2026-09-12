package com.hackathonteam7.mainprojectbackend.auth;

import com.hackathonteam7.mainprojectbackend.auth.dto.UserRegisterRequest;
import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.user.User;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserRegistrationService service;

    @Test
    void translatesConcurrentEmailConstraintFailure() {
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate", new ConstraintViolationException("duplicate", new SQLException(), "users.uq_users_email")));
        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.EMAIL_DUPLICATED));
    }

    @Test
    void doesNotMislabelUnrelatedDatabaseFailuresAsDuplicateEmail() {
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        var failure = new DataIntegrityViolationException("unrelated constraint",
                new ConstraintViolationException("unrelated", new SQLException(), "another_constraint"));
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.register(request())).isSameAs(failure);
    }

    private UserRegisterRequest request() {
        return new UserRegisterRequest("여행자", "user@example.com", "a long passphrase");
    }
}
