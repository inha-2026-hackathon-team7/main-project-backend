package com.hackathonteam7.mainprojectbackend.auth.dto;

import com.hackathonteam7.mainprojectbackend.user.User;

public record UserRegisterResponse(Long userId, String name, String email) {

    public static UserRegisterResponse from(User user) {
        return new UserRegisterResponse(user.getId(), user.getName(), user.getEmail());
    }
}
