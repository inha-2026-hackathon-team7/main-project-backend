package com.hackathonteam7.mainprojectbackend.auth.dto;

import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank String organizationName,
        @NotNull OrganizationType organizationType,
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminPassword
) {
}
