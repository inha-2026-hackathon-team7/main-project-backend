package com.hackathonteam7.mainprojectbackend.auth;

import com.hackathonteam7.mainprojectbackend.auth.dto.LoginRequest;
import com.hackathonteam7.mainprojectbackend.auth.dto.LoginResponse;
import com.hackathonteam7.mainprojectbackend.auth.dto.RegisterRequest;
import com.hackathonteam7.mainprojectbackend.auth.dto.RegisterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/admin/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
