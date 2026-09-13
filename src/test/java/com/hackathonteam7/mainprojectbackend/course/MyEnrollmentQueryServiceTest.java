package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.CoursePlaceCountRow;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentStampCountRow;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyEnrollmentQueryServiceTest {

    @Mock CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock CourseStampRepository courseStampRepository;
    @Mock CoursePlaceRepository coursePlaceRepository;
    MyEnrollmentQueryService service;

    @BeforeEach
    void setUp() {
        service = new MyEnrollmentQueryService(
                courseEnrollmentRepository, courseStampRepository, coursePlaceRepository);
    }

    @Test
    void omittedStatusReturnsActiveAndCompleteWithBatchAggregatedProgress() {
        CourseEnrollment active = enrollment(81L, 7L, "진행 코스", false);
        CourseEnrollment complete = enrollment(80L, 8L, "완주 코스", true);
        when(courseEnrollmentRepository.findAllByUserIdAndStatusInOrderByStartedAtDescIdDesc(
                42L, List.of(CourseEnrollmentStatus.ACTIVE, CourseEnrollmentStatus.COMPLETE)))
                .thenReturn(List.of(active, complete));
        when(courseStampRepository.countByEnrollmentIds(List.of(81L, 80L))).thenReturn(List.of(
                new EnrollmentStampCountRow(81L, 1), new EnrollmentStampCountRow(80L, 3)));
        when(coursePlaceRepository.countByCourseIds(List.of(7L, 8L))).thenReturn(List.of(
                new CoursePlaceCountRow(7L, 4), new CoursePlaceCountRow(8L, 3)));

        var result = service.list(42L, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).enrollmentId()).isEqualTo(81L);
        assertThat(result.get(0).status()).isEqualTo("active");
        assertThat(result.get(0).progress().done()).isEqualTo(1);
        assertThat(result.get(0).progress().total()).isEqualTo(4);
        assertThat(result.get(0).completedAt()).isNull();
        assertThat(result.get(1).courseName()).isEqualTo("완주 코스");
        assertThat(result.get(1).status()).isEqualTo("complete");
        assertThat(result.get(1).completedAt()).isNotNull();
    }

    @Test
    void statusFilterIsCaseInsensitiveAndEmptyResultSkipsCountQueries() {
        when(courseEnrollmentRepository.findAllByUserIdAndStatusInOrderByStartedAtDescIdDesc(
                42L, List.of(CourseEnrollmentStatus.COMPLETE))).thenReturn(List.of());

        assertThat(service.list(42L, " Complete ")).isEmpty();

        verify(courseEnrollmentRepository).findAllByUserIdAndStatusInOrderByStartedAtDescIdDesc(
                42L, List.of(CourseEnrollmentStatus.COMPLETE));
        verifyNoInteractions(courseStampRepository, coursePlaceRepository);
    }

    @Test
    void abandonedAndUnknownStatusesAreRejected() {
        for (String status : List.of("abandoned", "unknown")) {
            assertThatThrownBy(() -> service.list(42L, status))
                    .isInstanceOfSatisfying(ApiException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED));
        }
        verifyNoInteractions(courseEnrollmentRepository, courseStampRepository, coursePlaceRepository);
    }

    private CourseEnrollment enrollment(Long enrollmentId, Long courseId, String name, boolean complete) {
        Organization organization = Organization.builder().name("재단").type(OrganizationType.FACILITY).build();
        Course course = Course.builder()
                .organization(organization)
                .name(name)
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(true)
                .build();
        ReflectionTestUtils.setField(course, "id", courseId);
        User user = User.builder().name("사용자").email("user@example.com")
                .password("hash").role(Role.USER).build();
        CourseEnrollment enrollment = CourseEnrollment.builder().course(course).user(user).build();
        ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
        ReflectionTestUtils.setField(enrollment, "startedAt", LocalDateTime.of(2026, 9, 13, 10, 0));
        if (complete) {
            enrollment.complete();
            ReflectionTestUtils.setField(enrollment, "completedAt", LocalDateTime.of(2026, 9, 13, 11, 0));
        }
        return enrollment;
    }
}
