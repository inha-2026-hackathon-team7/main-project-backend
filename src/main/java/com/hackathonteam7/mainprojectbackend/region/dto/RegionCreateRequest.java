package com.hackathonteam7.mainprojectbackend.region.dto;

import jakarta.validation.constraints.NotBlank;

public record RegionCreateRequest(@NotBlank String name, String type) {
}
