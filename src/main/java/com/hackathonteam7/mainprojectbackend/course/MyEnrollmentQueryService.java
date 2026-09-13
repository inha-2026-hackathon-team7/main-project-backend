package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.common.error.FieldErrorDetail;
import com.hackathonteam7.mainprojectbackend.course.dto.user.CoursePlaceCountRow;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentStampCountRow;
import com.hackathonteam7.mainprojectbackend.course.dto.user.MyEnrollmentItem;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyEnrollmentQueryService {

    private static final List<CourseEnrollmentStatus> VISIBLE_STATUSES =
            List.of(CourseEnrollmentStatus.ACTIVE, CourseEnrollmentStatus.COMPLETE);

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseStampRepository courseStampRepository;
    private final CoursePlaceRepository coursePlaceRepository;

    public List<MyEnrollmentItem> list(Long userId, String status) {
        List<CourseEnrollmentStatus> statuses = parseStatuses(status);
        List<CourseEnrollment> enrollments = courseEnrollmentRepository
                .findAllByUserIdAndStatusInOrderByStartedAtDescIdDesc(userId, statuses);
        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> enrollmentIds = enrollments.stream().map(CourseEnrollment::getId).toList();
        List<Long> courseIds = enrollments.stream()
                .map(enrollment -> enrollment.getCourse().getId())
                .distinct()
                .toList();
        Map<Long, Long> stampCounts = courseStampRepository.countByEnrollmentIds(enrollmentIds).stream()
                .collect(Collectors.toMap(EnrollmentStampCountRow::enrollmentId, EnrollmentStampCountRow::count));
        Map<Long, Long> placeCounts = coursePlaceRepository.countByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(CoursePlaceCountRow::courseId, CoursePlaceCountRow::count));

        return enrollments.stream()
                .map(enrollment -> MyEnrollmentItem.from(
                        enrollment,
                        stampCounts.getOrDefault(enrollment.getId(), 0L),
                        placeCounts.getOrDefault(enrollment.getCourse().getId(), 0L)))
                .toList();
    }

    private List<CourseEnrollmentStatus> parseStatuses(String value) {
        if (value == null || value.isBlank()) {
            return VISIBLE_STATUSES;
        }
        try {
            CourseEnrollmentStatus status = CourseEnrollmentStatus.valueOf(value.strip().toUpperCase(Locale.ROOT));
            if (!VISIBLE_STATUSES.contains(status)) {
                throw new IllegalArgumentException();
            }
            return List.of(status);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    List.of(new FieldErrorDetail("status", "active, complete 중 하나여야 합니다."))
            );
        }
    }
}
