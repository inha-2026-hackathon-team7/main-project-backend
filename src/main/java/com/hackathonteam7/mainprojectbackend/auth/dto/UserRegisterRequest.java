package com.hackathonteam7.mainprojectbackend.auth.dto;

import com.hackathonteam7.mainprojectbackend.auth.validation.RegistrationPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.hibernate.validator.constraints.CodePointLength;

public record UserRegisterRequest(
        @NotBlank @CodePointLength(min = 1, max = 50) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @RegistrationPassword String password
) {
    public UserRegisterRequest {
        name = name == null ? null : name.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
