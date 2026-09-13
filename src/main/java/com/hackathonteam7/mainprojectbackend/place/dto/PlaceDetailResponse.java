package com.hackathonteam7.mainprojectbackend.place.dto;

import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.place.Place;
import java.math.BigDecimal;
import java.util.List;

public record PlaceDetailResponse(
        Long id,
        String name,
        Long regionId,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        String description,
        String imageUrl,
        String qrcodeString,
        List<CourseRef> referencingCourses
) {
    public record CourseRef(Long id, String name, CourseStatus status) {
        public static CourseRef from(Course course) {
            return new CourseRef(course.getId(), course.getName(), course.getStatus());
        }
    }

    public static PlaceDetailResponse from(Place place, List<Course> referencingCourses) {
        return new PlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getRegion().getId(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory(),
                place.getDescription(),
                place.getImageUrl(),
                place.getQrcodeString(),
                referencingCourses.stream().map(CourseRef::from).toList()
        );
    }
}
