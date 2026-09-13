package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.CoursePlaceCountRow;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListQuery;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseThumbnailRow;
import com.hackathonteam7.mainprojectbackend.course.policy.GeoDistancePolicy;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
import com.hackathonteam7.mainprojectbackend.reward.Reward;
import com.hackathonteam7.mainprojectbackend.reward.RewardKind;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCourseQueryServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock CoursePlaceRepository coursePlaceRepository;
    @Mock CourseEnrollmentRepository courseEnrollmentRepository;
    UserCourseQueryService service;

    Organization organization;
    Reward reward;
    Course orderedCourse;
    Course freeCourse;

    @BeforeEach
    void setUp() {
        service = new UserCourseQueryService(
                courseRepository, coursePlaceRepository, courseEnrollmentRepository, new GeoDistancePolicy());
        organization = Organization.builder().name("재단").type(OrganizationType.FACILITY).build();
        ReflectionTestUtils.setField(organization, "id", 2L);
        reward = Reward.builder().organization(organization).name("완주 포인트").kind(RewardKind.POINT).stock(100).build();
        ReflectionTestUtils.setField(reward, "id", 3L);
        orderedCourse = course(7L, "순서 코스", true);
        freeCourse = course(8L, "자유 코스", false);
    }

    @Test
    void listMapsLowercaseValuesAndSelectsThumbnailByCourseRule() {
        var query = UserCourseListQuery.of(2L, 5L, "official", 0, 20);
        when(courseRepository.findUserCourses(eq(CourseStatus.PUBLISHED), eq(2L), eq(5L),
                eq(CourseType.OFFICIAL), any())).thenReturn(new PageImpl<>(List.of(orderedCourse, freeCourse)));
        when(coursePlaceRepository.findThumbnailRowsByCourseIds(List.of(7L, 8L))).thenReturn(List.of(
                thumbnailRow(7L, true, 101L, 2, "ordered-second.jpg"),
                thumbnailRow(7L, true, 102L, 1, "ordered-first.jpg"),
                thumbnailRow(8L, false, 202L, 1, "free-newer.jpg"),
                thumbnailRow(8L, false, 201L, 2, "free-older.jpg")));
        when(coursePlaceRepository.countByCourseIds(List.of(7L, 8L))).thenReturn(List.of(
                new CoursePlaceCountRow(7L, 3L),
                new CoursePlaceCountRow(8L, 2L)));

        var result = service.list(query, null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo("official");
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("ordered-first.jpg");
        assertThat(result.get(0).rewardSummary().kind()).isEqualTo("point");
        assertThat(result.get(0).regionName()).isEqualTo("성수·서울숲");
        assertThat(result.get(0).durationMinutes()).isEqualTo(60);
        assertThat(result.get(0).distanceMeters()).isNull();
        assertThat(result.get(1).thumbnailUrl()).isEqualTo("free-older.jpg");
        assertThat(result.get(1).durationMinutes()).isEqualTo(40);
    }

    @Test
    void listComputesDistanceWhenUserLocationProvided() {
        var query = UserCourseListQuery.of(null, null, null, 0, 20);
        when(courseRepository.findUserCourses(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(orderedCourse)));
        when(coursePlaceRepository.findThumbnailRowsByCourseIds(List.of(7L))).thenReturn(List.of(
                thumbnailRow(7L, true, 101L, 1, "place.jpg")));
        when(coursePlaceRepository.countByCourseIds(List.of(7L))).thenReturn(List.of(new CoursePlaceCountRow(7L, 1L)));

        var result = service.list(query, new BigDecimal("0"), new BigDecimal("127"));

        assertThat(result.get(0).distanceMeters()).isNotNull().isEqualTo(0.0);
    }

    @Test
    void emptyListDoesNotQueryCoursePlaces() {
        when(courseRepository.findUserCourses(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.list(UserCourseListQuery.of(null, null, null, 0, 20), null, null)).isEmpty();
        verifyNoInteractions(coursePlaceRepository);
    }

    @Test
    void detailReturnsOnlyActiveEnrollmentForCurrentUserAndIncrementsViewCount() {
        User participant = User.builder().name("사용자").email("user@example.com")
                .password("hash").role(Role.USER).build();
        CourseEnrollment enrollment = CourseEnrollment.builder().course(orderedCourse).user(participant).build();
        ReflectionTestUtils.setField(enrollment, "id", 81L);
        when(courseRepository.findByIdAndStatus(7L, CourseStatus.PUBLISHED)).thenReturn(Optional.of(orderedCourse));
        when(coursePlaceRepository.findUserPlaceItemsByCourseId(7L)).thenReturn(List.of(
                new UserCoursePlaceItem(11L, 12L, "서울숲", BigDecimal.ONE, BigDecimal.TEN, 1, null, null)));
        when(coursePlaceRepository.findThumbnailRowsByCourseIds(List.of(7L))).thenReturn(List.of(
                thumbnailRow(7L, true, 11L, 1, null)));
        when(courseEnrollmentRepository.findByCourseIdAndUserIdAndStatus(
                7L, 42L, CourseEnrollmentStatus.ACTIVE)).thenReturn(Optional.of(enrollment));
        when(courseRepository.increaseViewCount(7L, CourseStatus.PUBLISHED)).thenReturn(1);

        var result = service.getDetail(7L, 42L, null, null);

        assertThat(result.myEnrollmentId()).isEqualTo(81L);
        assertThat(result.places()).hasSize(1);
        assertThat(result.reward().kind()).isEqualTo("point");
        assertThat(result.regionName()).isEqualTo("성수·서울숲");
        assertThat(result.durationMinutes()).isEqualTo(20);
        verify(courseRepository).increaseViewCount(7L, CourseStatus.PUBLISHED);
    }

    @Test
    void anonymousDetailDoesNotQueryEnrollment() {
        when(courseRepository.findByIdAndStatus(7L, CourseStatus.PUBLISHED)).thenReturn(Optional.of(orderedCourse));
        when(coursePlaceRepository.findUserPlaceItemsByCourseId(7L)).thenReturn(List.of());
        when(coursePlaceRepository.findThumbnailRowsByCourseIds(List.of(7L))).thenReturn(List.of());
        when(courseRepository.increaseViewCount(7L, CourseStatus.PUBLISHED)).thenReturn(1);

        var result = service.getDetail(7L, null, null, null);
        assertThat(result.myEnrollmentId()).isNull();
        assertThat(result.regionName()).isNull();
        assertThat(result.durationMinutes()).isNull();
        verifyNoInteractions(courseEnrollmentRepository);
    }

    @Test
    void missingOrNoLongerPublishedCourseReturnsCourseNotFound() {
        when(courseRepository.findByIdAndStatus(99L, CourseStatus.PUBLISHED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDetail(99L, null, null, null))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_FOUND));
        verify(courseRepository, never()).increaseViewCount(any(), any());
    }

    private UserCourseThumbnailRow thumbnailRow(Long courseId, boolean ordered, Long coursePlaceId, int visitOrder, String imageUrl) {
        return new UserCourseThumbnailRow(
                courseId, ordered, coursePlaceId, visitOrder, imageUrl,
                1L, "성수·서울숲", new BigDecimal("0"), new BigDecimal("127"));
    }

    private Course course(Long id, String name, boolean ordered) {
        Course course = Course.builder()
                .organization(organization)
                .reward(reward)
                .name(name)
                .description("설명")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(ordered)
                .build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}
