package com.hackathonteam7.mainprojectbackend.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class RegistrationPasswordValidator implements ConstraintValidator<RegistrationPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        // BCrypt의 한도를 글자 수로 검사하면 한글·이모지 비밀번호가 잘릴 수 있다.
        return value.codePointCount(0, value.length()) >= 8
                && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
