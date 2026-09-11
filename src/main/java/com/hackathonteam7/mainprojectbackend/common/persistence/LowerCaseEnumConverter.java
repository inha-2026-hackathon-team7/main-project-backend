package com.hackathonteam7.mainprojectbackend.common.persistence;

import jakarta.persistence.AttributeConverter;

/**
 * DB 는 MySQL ENUM/CHECK 제약을 소문자 값으로 정의한다 (V1__init.sql, V2__admin_additions.sql).
 * Java 쪽은 관례대로 대문자 상수를 쓰고, 이 컨버터가 대문자<->소문자를 왕복 변환한다.
 */
public abstract class LowerCaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> enumClass;

    protected LowerCaseEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Enum.valueOf(enumClass, dbData.toUpperCase());
    }
}
