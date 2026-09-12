package com.hackathonteam7.mainprojectbackend.auth;

import com.hackathonteam7.mainprojectbackend.auth.dto.LoginRequest;
import com.hackathonteam7.mainprojectbackend.auth.dto.UserRegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest
    @MethodSource("passwords")
    void validatesCharacterAndUtf8ByteBoundaries(String password, boolean valid) {
        var request = new UserRegisterRequest("여행자", "traveler@example.com", password);
        assertThat(validator.validate(request).isEmpty()).isEqualTo(valid);
    }

    static Stream<Arguments> passwords() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of(" ".repeat(8), false),
                Arguments.of("a".repeat(7), false),
                Arguments.of("a".repeat(8), true),
                Arguments.of("a".repeat(72), true),
                Arguments.of("a".repeat(73), false),
                Arguments.of("가".repeat(24), true),
                Arguments.of("가".repeat(25), false),
                Arguments.of("😀".repeat(7), false),
                Arguments.of("😀".repeat(8), true),
                Arguments.of(" a long passphrase ", true)
        );
    }

    @Test
    void normalizesIdentityButPreservesPassword() {
        var request = new UserRegisterRequest("  여행자  ", "  Traveler@Example.COM  ", " a long passphrase ");
        assertThat(request.name()).isEqualTo("여행자");
        assertThat(request.email()).isEqualTo("traveler@example.com");
        assertThat(request.password()).isEqualTo(" a long passphrase ");
        assertThat(validator.validate(request)).isEmpty();
        var login = new LoginRequest("  Traveler@Example.COM  ", request.password());
        assertThat(login.email()).isEqualTo(request.email());
        assertThat(login.password()).isEqualTo(request.password());
    }

    @Test
    void preservesEmailDotsAndPlusTag() {
        var request = new UserRegisterRequest("여행자", " First.Last+Trip@Example.com ", "a long passphrase");
        assertThat(request.email()).isEqualTo("first.last+trip@example.com");
    }

    @Test
    void rejectsMissingAndOversizedNamesAndEmails() {
        for (String name : new String[]{null, "", "   ", "가".repeat(51)}) {
            assertThat(validator.validate(new UserRegisterRequest(name, "user@example.com", "a long passphrase")))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }
        for (String email : new String[]{null, "", "   ", "not-an-email", "a".repeat(250) + "@example.com"}) {
            assertThat(validator.validate(new UserRegisterRequest("여행자", email, "a long passphrase")))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }
        assertThat(validator.validate(new UserRegisterRequest("😀".repeat(50), "user@example.com", "a long passphrase")))
                .isEmpty();
    }
}
