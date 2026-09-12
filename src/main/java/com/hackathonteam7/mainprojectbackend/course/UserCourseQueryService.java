package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseDetailResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListQuery;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseThumbnailRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCourseQueryService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    public List<UserCourseListItem> list(UserCourseListQuery query) {
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "id"));
        Page<Course> courses = courseRepository.findUserCourses(
                CourseStatus.PUBLISHED,
                query.organizationId(),
                query.regionId(),
                query.type(),
                pageable
        );

        if (courses.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, UserCourseThumbnailRow> thumbnailByCourse = selectThumbnails(
                coursePlaceRepository.findThumbnailRowsByCourseIds(courseIds));

        return courses.stream()
                .map(course -> {
                    UserCourseThumbnailRow thumbnail = thumbnailByCourse.get(course.getId());
                    return UserCourseListItem.from(course, thumbnail == null ? null : thumbnail.imageUrl());
                })
                .toList();
    }

    @Transactional
    public UserCourseDetailResponse getDetail(Long courseId, Long userId) {
        Course course = courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
        List<UserCoursePlaceItem> places = coursePlaceRepository.findUserPlaceItemsByCourseId(courseId);
        Long enrollmentId = findActiveEnrollmentId(courseId, userId);
        UserCourseDetailResponse response = UserCourseDetailResponse.from(course, places, enrollmentId);

        if (courseRepository.increaseViewCount(courseId, CourseStatus.PUBLISHED) != 1) {
            throw new ApiException(ErrorCode.COURSE_NOT_FOUND);
        }
        return response;
    }

    private Long findActiveEnrollmentId(Long courseId, Long userId) {
        if (userId == null) {
            return null;
        }
        return courseEnrollmentRepository
                .findByCourseIdAndUserIdAndStatus(courseId, userId, CourseEnrollmentStatus.ACTIVE)
                .map(CourseEnrollment::getId)
                .orElse(null);
    }

    private Map<Long, UserCourseThumbnailRow> selectThumbnails(List<UserCourseThumbnailRow> rows) {
        Map<Long, UserCourseThumbnailRow> selected = new HashMap<>();
        for (UserCourseThumbnailRow row : rows) {
            UserCourseThumbnailRow current = selected.get(row.courseId());
            if (row.precedes(current)) {
                selected.put(row.courseId(), row);
            }
        }
        return selected;
    }
}
