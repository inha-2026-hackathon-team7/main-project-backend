package com.hackathonteam7.mainprojectbackend.auth.dto;

public record RegisterResponse(Long organizationId, Long userId, String accessToken) {
}
