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
                .andExpect(jsonPath("$.completed_at").doesNotExist())
                .andExpect(jsonPath("$.course.id").value(course.getId()))
                .andExpect(jsonPath("$.course.name").value("참가 테스트 코스"))
                .andExpect(jsonPath("$.places", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.places[0].stamped").value(false))
                .andExpect(jsonPath("$.reward").doesNotExist());

        CourseEnrollment enrollment = courseEnrollmentRepository.findById(enrollmentId).orElseThrow();
        courseStampRepository.save(CourseStamp.builder()
                .courseEnrollment(enrollment)
                .coursePlace(firstCoursePlace)
                .build());

        mockMvc.perform(get("/enrollments/{enrollmentId}", enrollmentId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stamped_course_place_ids[0]").value(firstCoursePlace.getId()))
                .andExpect(jsonPath("$.next_place.course_place_id").value(secondCoursePlace.getId()))
                .andExpect(jsonPath("$.places[0].stamped").value(true))
                .andExpect(jsonPath("$.places[1].stamped").value(false));

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

    @Test
    void abandonEndsActiveEnrollmentButNotACompletedOne() throws Exception {
        Region region = regionRepository.save(
                Region.builder().organization(organization).name("포기 테스트 지역 " + UUID.randomUUID()).build());
        Place onlyPlace = place(region, "포기 테스트 장소");
        Course activeCourse = courseRepository.save(Course.builder()
                .organization(organization).name("포기용 코스").type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED).isOrdered(false).build());
        coursePlaceRepository.save(CoursePlace.builder().course(activeCourse).place(onlyPlace).visitOrder(1).build());
        User participant = user("abandon-participant");
        User other = user("abandon-other");
        String participantToken = token(participant);

        var started = mockMvc.perform(post("/courses/{courseId}/enrollments", activeCourse.getId())
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        long enrollmentId = objectMapper.readTree(started.getContentAsString()).get("enrollment_id").asLong();

        mockMvc.perform(post("/enrollments/{id}/abandon", enrollmentId)
                        .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_FOUND"));

        mockMvc.perform(post("/enrollments/{id}/abandon", enrollmentId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollment_id").value(enrollmentId))
                .andExpect(jsonPath("$.status").value("abandoned"));
        assertThat(courseEnrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
                .isEqualTo(CourseEnrollmentStatus.ABANDONED);

        // 이미 포기한 참가를 다시 포기해도 멱등하게 같은 상태를 반환한다.
        mockMvc.perform(post("/enrollments/{id}/abandon", enrollmentId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("abandoned"));

        Course completableCourse = courseRepository.save(Course.builder()
                .organization(organization).name("완주용 코스").type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED).isOrdered(false).build());
        coursePlaceRepository.save(
                CoursePlace.builder().course(completableCourse).place(onlyPlace).visitOrder(1).build());
        CourseEnrollment completedEnrollment = courseEnrollmentRepository.save(
                CourseEnrollment.builder().course(completableCourse).user(participant).build());
        completedEnrollment.complete();
        courseEnrollmentRepository.save(completedEnrollment);

        mockMvc.perform(post("/enrollments/{id}/abandon", completedEnrollment.getId())
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_ALREADY_ENDED"));
    }

    @Test
    void restartingAnAbandonedEnrollmentResetsTheSameRowAndClearsOldStamps() throws Exception {
        Region region = regionRepository.save(
                Region.builder().organization(organization).name("재시작 테스트 지역 " + UUID.randomUUID()).build());
        Place first = place(region, "재시작 테스트 장소1");
        Place second = place(region, "재시작 테스트 장소2");
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("재시작용 코스").type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED).isOrdered(false).build());
        CoursePlace firstCoursePlace = coursePlaceRepository.save(
                CoursePlace.builder().course(course).place(first).visitOrder(1).build());
        coursePlaceRepository.save(CoursePlace.builder().course(course).place(second).visitOrder(2).build());
        User participant = user("restart-participant");
        String token = token(participant);

        var started = mockMvc.perform(post("/courses/{courseId}/enrollments", course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        long enrollmentId = objectMapper.readTree(started.getContentAsString()).get("enrollment_id").asLong();

        // 포기 전에 한 곳을 스탬프해둔다 (재시작 후 이 기록이 남아있으면 안 된다).
        CourseEnrollment enrollment = courseEnrollmentRepository.findById(enrollmentId).orElseThrow();
        courseStampRepository.save(CourseStamp.builder()
                .courseEnrollment(enrollment).coursePlace(firstCoursePlace).build());
        assertThat(courseStampRepository.countByCourseEnrollmentId(enrollmentId)).isEqualTo(1);

        mockMvc.perform(post("/enrollments/{id}/abandon", enrollmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("abandoned"));

        // 재시작: 같은 코스에 다시 참가하면 같은 enrollment row 가 ACTIVE 로 리셋되고, 이전 스탬프는 지워진다.
        mockMvc.perform(post("/courses/{courseId}/enrollments", course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollment_id").value(enrollmentId))
                .andExpect(jsonPath("$.status").value("active"));

        assertThat(courseEnrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
                .isEqualTo(CourseEnrollmentStatus.ACTIVE);
        assertThat(courseEnrollmentRepository.findById(enrollmentId).orElseThrow().getCompletedAt()).isNull();
        assertThat(courseStampRepository.countByCourseEnrollmentId(enrollmentId)).isZero();

        mockMvc.perform(get("/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stamped_course_place_ids").isEmpty())
                .andExpect(jsonPath("$.total_places").value(2));
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
