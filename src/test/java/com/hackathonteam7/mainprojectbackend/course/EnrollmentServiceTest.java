package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock CoursePlaceRepository coursePlaceRepository;
    @Mock CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock UserRepository userRepository;
    EnrollmentService service;
    Course publishedCourse;
    User user;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(
                courseRepository, coursePlaceRepository, courseEnrollmentRepository, userRepository);
        Organization organization = Organization.builder().name("재단").type(OrganizationType.FACILITY).build();
        publishedCourse = Course.builder()
                .organization(organization)
                .name("공개 코스")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(true)
                .build();
        ReflectionTestUtils.setField(publishedCourse, "id", 7L);
        user = User.builder().name("사용자").email("user@example.com").password("hash").role(Role.USER).build();
        ReflectionTestUtils.setField(user, "id", 42L);
    }

    @Test
    void createsActiveEnrollmentForPublishedCourseWithPlaces() {
        when(courseRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(publishedCourse));
        when(courseEnrollmentRepository.findByCourseIdAndUserId(7L, 42L)).thenReturn(Optional.empty());
        when(coursePlaceRepository.countByCourseId(7L)).thenReturn(2L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(courseEnrollmentRepository.save(any())).thenAnswer(invocation -> {
            CourseEnrollment enrollment = invocation.getArgument(0);
            ReflectionTestUtils.setField(enrollment, "id", 81L);
            ReflectionTestUtils.setField(enrollment, "startedAt", LocalDateTime.of(2026, 9, 13, 10, 30));
            return enrollment;
        });

        var result = service.start(7L, 42L);

        assertThat(result.created()).isTrue();
        assertThat(result.response().enrollmentId()).isEqualTo(81L);
        assertThat(result.response().status()).isEqualTo("active");
    }

    @Test
    void reusesExistingEnrollmentWithoutResettingCompletedState() {
        CourseEnrollment enrollment = CourseEnrollment.builder().course(publishedCourse).user(user).build();
        ReflectionTestUtils.setField(enrollment, "id", 81L);
        ReflectionTestUtils.setField(enrollment, "startedAt", LocalDateTime.of(2026, 9, 1, 9, 0));
        enrollment.complete();
        when(courseRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(publishedCourse));
        when(courseEnrollmentRepository.findByCourseIdAndUserId(7L, 42L)).thenReturn(Optional.of(enrollment));

        var result = service.start(7L, 42L);

        assertThat(result.created()).isFalse();
        assertThat(result.response().enrollmentId()).isEqualTo(81L);
        assertThat(result.response().status()).isEqualTo("complete");
        verify(courseEnrollmentRepository, never()).save(any());
        verifyNoInteractions(coursePlaceRepository, userRepository);
    }

    @Test
    void rejectsNewEnrollmentForUnpublishedOrEmptyCourse() {
        Course draft = Course.builder()
                .organization(publishedCourse.getOrganization())
                .name("초안")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(draft, "id", 8L);
        when(courseRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(draft));
        when(courseEnrollmentRepository.findByCourseIdAndUserId(8L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(8L, 42L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_FOUND));

        when(courseRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(publishedCourse));
        when(courseEnrollmentRepository.findByCourseIdAndUserId(7L, 42L)).thenReturn(Optional.empty());
        when(coursePlaceRepository.countByCourseId(7L)).thenReturn(0L);

        assertThatThrownBy(() -> service.start(7L, 42L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_READY));
        verify(userRepository, never()).findById(any());
    }
}
