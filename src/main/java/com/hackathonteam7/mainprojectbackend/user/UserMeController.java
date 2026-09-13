package com.hackathonteam7.mainprojectbackend.user;

import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import com.hackathonteam7.mainprojectbackend.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserMeController {

    private final UserProfileService userProfileService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping
    public UserProfileResponse getProfile(@AuthenticationPrincipal PrincipalUser me) {
        return userProfileService.getProfile(me.userId());
    }
}
