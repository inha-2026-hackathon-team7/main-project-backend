package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.CoursePlaceCountRow;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseDetailResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListQuery;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseThumbnailRow;
import com.hackathonteam7.mainprojectbackend.course.policy.GeoDistancePolicy;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    /** 실측 데이터가 없어 장소 수 기반으로 추정한다 (장소당 20분). */
    private static final int MINUTES_PER_PLACE = 20;

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final GeoDistancePolicy geoDistancePolicy;

    public List<UserCourseListItem> list(UserCourseListQuery query, BigDecimal userLatitude, BigDecimal userLongitude) {
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
        Map<Long, UserCourseThumbnailRow> representativeByCourse = selectRepresentativePlaces(
                coursePlaceRepository.findThumbnailRowsByCourseIds(courseIds));
        Map<Long, Long> placeCountByCourse = coursePlaceRepository.countByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(CoursePlaceCountRow::courseId, CoursePlaceCountRow::count));

        return courses.stream()
                .map(course -> {
                    UserCourseThumbnailRow representative = representativeByCourse.get(course.getId());
                    Double distance = distanceMetersTo(representative, userLatitude, userLongitude);
                    Integer duration = estimateDurationMinutes(placeCountByCourse.getOrDefault(course.getId(), 0L));
                    return UserCourseListItem.from(course, representative, distance, duration);
                })
                .toList();
    }

    @Transactional
    public UserCourseDetailResponse getDetail(Long courseId, Long userId, BigDecimal userLatitude, BigDecimal userLongitude) {
        Course course = courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
        List<UserCoursePlaceItem> places = coursePlaceRepository.findUserPlaceItemsByCourseId(courseId);
        Long enrollmentId = findActiveEnrollmentId(courseId, userId);
        UserCourseThumbnailRow representative = selectRepresentativePlaces(
                coursePlaceRepository.findThumbnailRowsByCourseIds(List.of(courseId))).get(courseId);
        Double distance = distanceMetersTo(representative, userLatitude, userLongitude);
        Integer duration = estimateDurationMinutes((long) places.size());

        UserCourseDetailResponse response = UserCourseDetailResponse.from(
                course, places, enrollmentId, representative, distance, duration);

        if (courseRepository.increaseViewCount(courseId, CourseStatus.PUBLISHED) != 1) {
            throw new ApiException(ErrorCode.COURSE_NOT_FOUND);
        }
        return response;
    }

    private Double distanceMetersTo(UserCourseThumbnailRow representative, BigDecimal userLatitude, BigDecimal userLongitude) {
        if (representative == null || userLatitude == null || userLongitude == null) {
            return null;
        }
        return geoDistancePolicy.distanceMeters(
                representative.latitude(), representative.longitude(), userLatitude, userLongitude);
    }

    private Integer estimateDurationMinutes(long placeCount) {
        return placeCount > 0 ? (int) (placeCount * MINUTES_PER_PLACE) : null;
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

    private Map<Long, UserCourseThumbnailRow> selectRepresentativePlaces(List<UserCourseThumbnailRow> rows) {
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
