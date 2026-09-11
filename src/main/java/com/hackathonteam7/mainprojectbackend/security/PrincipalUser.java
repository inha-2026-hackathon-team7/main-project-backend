package com.hackathonteam7.mainprojectbackend.security;

import com.hackathonteam7.mainprojectbackend.user.Role;

public record PrincipalUser(Long userId, Long orgId, Role role) {
}
