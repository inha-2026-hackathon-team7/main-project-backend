package com.hackathonteam7.mainprojectbackend.auth;

import com.hackathonteam7.mainprojectbackend.auth.dto.UserRegisterRequest;
import com.hackathonteam7.mainprojectbackend.auth.dto.UserRegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserAuthController {

    private final UserRegistrationService userRegistrationService;

    @Operation(summary = "일반 사용자 회원가입")
    @SecurityRequirements
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegisterResponse register(@Valid @RequestBody UserRegisterRequest request) {
        return userRegistrationService.register(request);
    }
}
