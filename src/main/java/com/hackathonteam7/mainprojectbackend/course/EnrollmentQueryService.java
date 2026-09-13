package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentProgressResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentProgressResponse.EnrollmentCourseSummary;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentProgressResponse.EnrollmentPlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseRewardDetail;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentQueryService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseStampRepository courseStampRepository;

    public EnrollmentProgressResponse getProgress(Long enrollmentId, Long userId) {
        CourseEnrollment enrollment = courseEnrollmentRepository.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));
        List<UserCoursePlaceItem> places = coursePlaceRepository
                .findUserPlaceItemsByCourseId(enrollment.getCourse().getId());
        List<Long> stampedIds = courseStampRepository.findStampedCoursePlaceIds(enrollmentId);
        Set<Long> stamped = new HashSet<>(stampedIds);

        return new EnrollmentProgressResponse(
                enrollment.getStatus().name().toLowerCase(Locale.ROOT),
                stampedIds,
                places.size(),
                findNextPlace(enrollment, places, stampedIds),
                enrollment.getCompletedAt(),
                EnrollmentCourseSummary.from(enrollment.getCourse()),
                places.stream().map(place -> EnrollmentPlaceItem.from(place, stamped.contains(place.coursePlaceId()))).toList(),
                UserCourseRewardDetail.from(enrollment.getCourse().getReward())
        );
    }

    private UserCoursePlaceItem findNextPlace(
            CourseEnrollment enrollment,
            List<UserCoursePlaceItem> places,
            List<Long> stampedIds
    ) {
        if (enrollment.getStatus() != CourseEnrollmentStatus.ACTIVE
                || !Boolean.TRUE.equals(enrollment.getCourse().getIsOrdered())) {
            return null;
        }
        Set<Long> stamped = new HashSet<>(stampedIds);
        return places.stream()
                .filter(place -> !stamped.contains(place.coursePlaceId()))
                .findFirst()
                .orElse(null);
    }
}
