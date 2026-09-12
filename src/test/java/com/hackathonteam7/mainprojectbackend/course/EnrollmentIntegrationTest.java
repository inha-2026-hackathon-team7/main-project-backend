package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.region.Region;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnrollmentIntegrationTest extends IntegrationTestSupport {

    @Autowired RegionRepository regionRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CoursePlaceRepository coursePlaceRepository;
    @Autowired CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired CourseStampRepository courseStampRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    void enrollmentLifecycleUsesRealDatabaseAndProtectsOwnershipAndCourseStructure() throws Exception {
        Region region = regionRepository.save(
                Region.builder().organization(organization).name("참가 테스트 지역 " + UUID.randomUUID()).build());
        Place first = place(region, "첫 장소");
        Place second = place(region, "두 번째 장소");
        Course course = courseRepository.save(Course.builder()
                .organization(organization)
                .name("참가 테스트 코스")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(true)
                .build());
        CoursePlace firstCoursePlace = coursePlaceRepository.save(
                CoursePlace.builder().course(course).place(first).visitOrder(1).build());
        CoursePlace secondCoursePlace = coursePlaceRepository.save(
                CoursePlace.builder().course(course).place(second).visitOrder(2).build());
        User participant = user("participant");
        User other = user("other");
        String participantToken = token(participant);
        String otherToken = token(other);

        var created = mockMvc.perform(post("/courses/{courseId}/enrollments", course.getId())
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.started_at").isNotEmpty())
                .andReturn().getResponse();
        long enrollmentId = objectMapper.readTree(created.getContentAsString()).get("enrollment_id").asLong();

        mockMvc.perform(post("/courses/{courseId}/enrollments", course.getId())
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollment_id").value(enrollmentId))
                .andExpect(jsonPath("$.status").value("active"));
        assertThat(courseEnrollmentRepository.countByCourseId(course.getId())).isEqualTo(1);

        mockMvc.perform(get("/enrollments/{enrollmentId}", enrollmentId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stamped_course_place_ids").isEmpty())
                .andExpect(jsonPath("$.total_places").value(2))
                .andExpect(jsonPath("$.next_place.course_place_id").value(firstCoursePlace.getId()))
                .andExpect(jsonPath("$.next_place.visit_order").value(1))
                .andExpect(jsonPath("$.completed_at").doesNotExist());

        CourseEnrollment enrollment = courseEnrollmentRepository.findById(enrollmentId).orElseThrow();
        courseStampRepository.save(CourseStamp.builder()
                .courseEnrollment(enrollment)
                .coursePlace(firstCoursePlace)
                .build());

        mockMvc.perform(get("/enrollments/{enrollmentId}", enrollmentId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stamped_course_place_ids[0]").value(firstCoursePlace.getId()))
                .andExpect(jsonPath("$.next_place.course_place_id").value(secondCoursePlace.getId()));

        mockMvc.perform(get("/enrollments/{enrollmentId}", enrollmentId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_FOUND"));

        mockMvc.perform(put("/admin/courses/{courseId}/places", course.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_STRUCTURE_LOCKED"));
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

    private User user(String prefix) {
        return userRepository.save(User.builder()
                .name(prefix)
                .email(prefix + "-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
    }

    private String token(User user) {
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), null);
    }
}
