package com.hackathonteam7.mainprojectbackend.review;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseReviewDecisionConverter extends LowerCaseEnumConverter<CourseReviewDecision> {

    public CourseReviewDecisionConverter() {
        super(CourseReviewDecision.class);
    }
}
