package com.hackathonteam7.mainprojectbackend.user;

import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollment;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollmentRepository;
import com.hackathonteam7.mainprojectbackend.course.CoursePlace;
import com.hackathonteam7.mainprojectbackend.course.CoursePlaceRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseStamp;
import com.hackathonteam7.mainprojectbackend.course.CourseStampRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseType;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.region.Region;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyPageIntegrationTest extends IntegrationTestSupport {

    @Autowired RegionRepository regionRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CoursePlaceRepository coursePlaceRepository;
    @Autowired CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired CourseStampRepository courseStampRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    void profileAndEnrollmentFiltersUseAuthenticatedUserAndRealProgressCounts() throws Exception {
        User participant = userRepository.save(User.builder()
                .name("마이페이지 사용자")
                .email("mypage-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
        String token = jwtTokenProvider.generateAccessToken(
                participant.getId(), participant.getEmail(), participant.getRole(), null);

        Region region = regionRepository.save(Region.builder()
                .organization(organization)
                .name("마이페이지 지역 " + UUID.randomUUID())
                .build());
        Place first = place(region, "첫 장소");
        Place second = place(region, "두 번째 장소");
        Course completeCourse = course("완주 코스");
        Course activeCourse = course("진행 코스");
        Course abandonedCourse = course("중단 코스");

        CoursePlace completeFirst = coursePlaceRepository.save(
                CoursePlace.builder().course(completeCourse).place(first).visitOrder(1).build());
        coursePlaceRepository.save(
                CoursePlace.builder().course(completeCourse).place(second).visitOrder(2).build());
        coursePlaceRepository.save(
                CoursePlace.builder().course(activeCourse).place(first).visitOrder(1).build());

        CourseEnrollment complete = enrollment(completeCourse, participant,
                LocalDateTime.of(2026, 9, 11, 9, 0));
        complete.complete();
        complete = courseEnrollmentRepository.save(complete);
        CourseEnrollment active = courseEnrollmentRepository.save(enrollment(activeCourse, participant,
                LocalDateTime.of(2026, 9, 12, 9, 0)));
        CourseEnrollment abandoned = enrollment(abandonedCourse, participant,
                LocalDateTime.of(2026, 9, 13, 9, 0));
        abandoned.abandon();
        courseEnrollmentRepository.save(abandoned);
        courseStampRepository.save(CourseStamp.builder()
                .courseEnrollment(complete)
                .coursePlace(completeFirst)
                .build());

        var profileResponse = mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(participant.getId()))
                .andExpect(jsonPath("$.name").value("마이페이지 사용자"))
                .andExpect(jsonPath("$.email").value(participant.getEmail()))
                .andReturn().getResponse();
        assertThat(objectMapper.readTree(profileResponse.getContentAsString()).size()).isEqualTo(3);

        var allResponse = mockMvc.perform(get("/users/me/enrollments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].enrollment_id").value(active.getId()))
                .andExpect(jsonPath("$[0].status").value("active"))
                .andExpect(jsonPath("$[0].progress.done").value(0))
                .andExpect(jsonPath("$[0].progress.total").value(1))
                .andExpect(jsonPath("$[1].enrollment_id").value(complete.getId()))
                .andExpect(jsonPath("$[1].status").value("complete"))
                .andExpect(jsonPath("$[1].progress.done").value(1))
                .andExpect(jsonPath("$[1].progress.total").value(2))
                .andReturn().getResponse();
        var activeJson = objectMapper.readTree(allResponse.getContentAsString()).get(0);
        assertThat(activeJson.has("completed_at")).isTrue();
        assertThat(activeJson.get("completed_at").isNull()).isTrue();

        mockMvc.perform(get("/users/me/enrollments")
                        .queryParam("status", "complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enrollment_id").value(complete.getId()));

        mockMvc.perform(get("/users/me/enrollments")
                        .queryParam("status", "abandoned")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("status"));
    }

    private CourseEnrollment enrollment(Course course, User user, LocalDateTime startedAt) {
        CourseEnrollment enrollment = CourseEnrollment.builder().course(course).user(user).build();
        ReflectionTestUtils.setField(enrollment, "startedAt", startedAt);
        return enrollment;
    }

    private Course course(String name) {
        return courseRepository.save(Course.builder()
                .organization(organization)
                .name(name)
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(true)
                .build());
    }

    private Place place(Region region, String name) {
        return placeRepository.save(Place.builder()
                .organization(organization)
                .region(region)
                .name(name)
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .qrcodeString(UUID.randomUUID().toString())
                .build());
    }
}
