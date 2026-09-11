package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseEnrollmentStatusConverter extends LowerCaseEnumConverter<CourseEnrollmentStatus> {

    public CourseEnrollmentStatusConverter() {
        super(CourseEnrollmentStatus.class);
    }
}
