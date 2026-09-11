package com.hackathonteam7.mainprojectbackend.organization;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrganizationMemberRoleConverter extends LowerCaseEnumConverter<OrganizationMemberRole> {

    public OrganizationMemberRoleConverter() {
        super(OrganizationMemberRole.class);
    }
}
