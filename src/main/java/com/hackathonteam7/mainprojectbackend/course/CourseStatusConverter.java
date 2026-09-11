package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseStatusConverter extends LowerCaseEnumConverter<CourseStatus> {

    public CourseStatusConverter() {
        super(CourseStatus.class);
    }
}
