package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentStartResponse;
import com.hackathonteam7.mainprojectbackend.user.User;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public EnrollmentStartResult start(Long courseId, Long userId) {
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));

        CourseEnrollment existing = courseEnrollmentRepository.findByCourseIdAndUserId(courseId, userId)
                .orElse(null);
        if (existing != null) {
            return new EnrollmentStartResult(EnrollmentStartResponse.from(existing), false);
        }

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (coursePlaceRepository.countByCourseId(courseId) == 0) {
            throw new ApiException(ErrorCode.COURSE_NOT_READY);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        CourseEnrollment enrollment = courseEnrollmentRepository.save(
                CourseEnrollment.builder().course(course).user(user).build());
        return new EnrollmentStartResult(EnrollmentStartResponse.from(enrollment), true);
    }

    public record EnrollmentStartResult(EnrollmentStartResponse response, boolean created) {
    }
}
