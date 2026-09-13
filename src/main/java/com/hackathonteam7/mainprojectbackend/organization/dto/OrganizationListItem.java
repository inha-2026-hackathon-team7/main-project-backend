package com.hackathonteam7.mainprojectbackend.organization.dto;

import com.hackathonteam7.mainprojectbackend.organization.Organization;
import java.util.Locale;

public record OrganizationListItem(Long id, String name, String type) {

    public static OrganizationListItem from(Organization organization) {
        return new OrganizationListItem(
                organization.getId(),
                organization.getName(),
                organization.getType().name().toLowerCase(Locale.ROOT)
        );
    }
}
