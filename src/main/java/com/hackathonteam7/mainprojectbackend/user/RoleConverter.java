package com.hackathonteam7.mainprojectbackend.user;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter extends LowerCaseEnumConverter<Role> {

    public RoleConverter() {
        super(Role.class);
    }
}
