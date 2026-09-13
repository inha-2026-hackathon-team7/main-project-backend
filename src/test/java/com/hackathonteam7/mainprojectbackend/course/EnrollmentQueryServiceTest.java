package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentQueryServiceTest {

    @Mock CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock CoursePlaceRepository coursePlaceRepository;
    @Mock CourseStampRepository courseStampRepository;
    EnrollmentQueryService service;
    CourseEnrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new EnrollmentQueryService(
                courseEnrollmentRepository, coursePlaceRepository, courseStampRepository);
        Organization organization = Organization.builder().name("재단").type(OrganizationType.FACILITY).build();
        Course course = Course.builder()
                .organization(organization)
                .name("순서 코스")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(true)
                .build();
        ReflectionTestUtils.setField(course, "id", 7L);
        User user = User.builder().name("사용자").email("user@example.com")
                .password("hash").role(Role.USER).build();
        enrollment = CourseEnrollment.builder().course(course).user(user).build();
        ReflectionTestUtils.setField(enrollment, "id", 81L);
    }

    @Test
    void returnsStampedIdsTotalAndFirstUnstampedPlaceForOrderedCourse() {
        UserCoursePlaceItem first = place(11L, 1);
        UserCoursePlaceItem second = place(12L, 2);
        UserCoursePlaceItem third = place(13L, 3);
        when(courseEnrollmentRepository.findByIdAndUserId(81L, 42L)).thenReturn(Optional.of(enrollment));
        when(coursePlaceRepository.findUserPlaceItemsByCourseId(7L)).thenReturn(List.of(first, second, third));
        when(courseStampRepository.findStampedCoursePlaceIds(81L)).thenReturn(List.of(11L));

        var result = service.getProgress(81L, 42L);

        assertThat(result.status()).isEqualTo("active");
        assertThat(result.stampedCoursePlaceIds()).containsExactly(11L);
        assertThat(result.totalPlaces()).isEqualTo(3);
        assertThat(result.nextPlace()).isEqualTo(second);
        assertThat(result.completedAt()).isNull();
        assertThat(result.course().id()).isEqualTo(7L);
        assertThat(result.course().name()).isEqualTo("순서 코스");
        assertThat(result.places()).hasSize(3);
        assertThat(result.places().get(0).stamped()).isTrue();
        assertThat(result.places().get(1).stamped()).isFalse();
        assertThat(result.reward()).isNull();
    }

    @Test
    void hidesExistenceOfAnotherUsersEnrollment() {
        when(courseEnrollmentRepository.findByIdAndUserId(81L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgress(81L, 99L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENROLLMENT_NOT_FOUND));
        verifyNoInteractions(coursePlaceRepository, courseStampRepository);
    }

    private UserCoursePlaceItem place(Long coursePlaceId, int order) {
        return new UserCoursePlaceItem(
                coursePlaceId,
                100L + coursePlaceId,
                order + "번째 장소",
                BigDecimal.ONE,
                BigDecimal.TEN,
                order,
                null,
                null
        );
    }
}
