package com.hackathonteam7.mainprojectbackend.auth.dto;

import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;

public record LoginResponse(String accessToken, UserSummary user) {

    public record UserSummary(Long id, String name, Role role) {
        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getName(), user.getRole());
        }
    }

    public static LoginResponse of(String accessToken, User user) {
        return new LoginResponse(accessToken, UserSummary.from(user));
    }
}
