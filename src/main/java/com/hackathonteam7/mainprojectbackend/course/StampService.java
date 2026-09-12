package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.StampRequest;
import com.hackathonteam7.mainprojectbackend.course.dto.user.StampResponse;
import com.hackathonteam7.mainprojectbackend.course.policy.GeoDistancePolicy;
import com.hackathonteam7.mainprojectbackend.course.policy.VisitOrderPolicy;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StampService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final PlaceRepository placeRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseStampRepository courseStampRepository;
    private final GeoDistancePolicy geoDistancePolicy;
    private final VisitOrderPolicy visitOrderPolicy;

    @Transactional
    public StampResponse stamp(Long enrollmentId, Long userId, StampRequest request) {
        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByIdAndUserIdForUpdate(enrollmentId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));

        Place place = placeRepository.findByQrcodeString(request.qrcodeString())
                .orElseThrow(() -> new ApiException(ErrorCode.QR_PLACE_NOT_FOUND));
        Long courseId = enrollment.getCourse().getId();
        CoursePlace coursePlace = coursePlaceRepository.findByCourseIdAndPlaceId(courseId, place.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.PLACE_NOT_IN_COURSE));

        if (courseStampRepository.existsByCourseEnrollmentIdAndCoursePlaceId(enrollmentId, coursePlace.getId())) {
            throw new ApiException(ErrorCode.STAMP_ALREADY_EXISTS);
        }
        if (enrollment.getStatus() != CourseEnrollmentStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ENROLLMENT_NOT_ACTIVE);
        }

        List<CoursePlace> coursePlaces = coursePlaceRepository.findAllByCourseIdOrderByVisitOrder(courseId);
        Set<Long> stampedIds = new HashSet<>(courseStampRepository.findStampedCoursePlaceIds(enrollmentId));
        visitOrderPolicy.validate(
                Boolean.TRUE.equals(enrollment.getCourse().getIsOrdered()),
                coursePlace.getId(),
                coursePlaces.stream().map(CoursePlace::getId).toList(),
                stampedIds
        );
        geoDistancePolicy.validate(
                place.getLatitude(),
                place.getLongitude(),
                request.latitude(),
                request.longitude()
        );

        CourseStamp stamp = courseStampRepository.saveAndFlush(
                CourseStamp.builder()
                        .courseEnrollment(enrollment)
                        .coursePlace(coursePlace)
                        .build()
        );
        long done = courseStampRepository.countByCourseEnrollmentId(enrollmentId);
        long total = coursePlaces.size();
        boolean completed = total > 0 && done == total;
        if (completed) {
            enrollment.complete();
        }

        return StampResponse.from(stamp, place.getName(), done, total, completed);
    }
}
