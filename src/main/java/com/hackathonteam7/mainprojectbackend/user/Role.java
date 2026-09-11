package com.hackathonteam7.mainprojectbackend.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    USER,
    ORGANIZATION;

    /** 프론트가 이 값으로 라우팅하므로 소문자 문자열로 내려준다 (CLAUDE.md §5.1). */
    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Role fromJson(String value) {
        return Role.valueOf(value.toUpperCase());
    }
}
