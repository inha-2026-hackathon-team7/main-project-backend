package com.hackathonteam7.mainprojectbackend.organization;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrganizationTypeConverter extends LowerCaseEnumConverter<OrganizationType> {

    public OrganizationTypeConverter() {
        super(OrganizationType.class);
    }
}
