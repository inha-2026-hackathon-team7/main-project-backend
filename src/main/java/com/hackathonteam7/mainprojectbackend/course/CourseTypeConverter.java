package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseTypeConverter extends LowerCaseEnumConverter<CourseType> {

    public CourseTypeConverter() {
        super(CourseType.class);
    }
}
