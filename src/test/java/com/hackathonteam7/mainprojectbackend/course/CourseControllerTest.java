package com.hackathonteam7.mainprojectbackend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

class CourseControllerTest extends IntegrationTestSupport {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    private Place placeA;
    private Place placeB;

    private void seedPlaces() {
        Region region = regionRepository.save(Region.builder().organization(organization).name("성수").build());
        placeA = placeRepository.save(Place.builder()
                .region(region).organization(organization).name("A")
                .latitude(new BigDecimal("37.1")).longitude(new BigDecimal("127.1"))
                .qrcodeString(UUID.randomUUID().toString()).build());
        placeB = placeRepository.save(Place.builder()
                .region(region).organization(organization).name("B")
                .latitude(new BigDecimal("37.2")).longitude(new BigDecimal("127.2"))
                .qrcodeString(UUID.randomUUID().toString()).build());
    }

    @Test
    void create_forcesOfficialTypeAndDraftStatus() throws Exception {
        String body = """
                {"name": "새 코스", "is_ordered": true}
                """;

        mockMvc.perform(post("/admin/courses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("OFFICIAL"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void replacePlaces_nonContiguousVisitOrder_returns400() throws Exception {
        seedPlaces();
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("코스").type(CourseType.OFFICIAL).status(CourseStatus.DRAFT).isOrdered(true).build());

        String body = """
                [{"place_id": %d, "visit_order": 1}, {"place_id": %d, "visit_order": 3}]
                """.formatted(placeA.getId(), placeB.getId());

        mockMvc.perform(put("/admin/courses/{id}/places", course.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VISIT_ORDER"));
    }

    @Test
    void replacePlaces_duplicatePlace_returns400() throws Exception {
        seedPlaces();
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("코스").type(CourseType.OFFICIAL).status(CourseStatus.DRAFT).isOrdered(true).build());

        String body = """
                [{"place_id": %d, "visit_order": 1}, {"place_id": %d, "visit_order": 2}]
                """.formatted(placeA.getId(), placeA.getId());

        mockMvc.perform(put("/admin/courses/{id}/places", course.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PLACE"));
    }

    @Test
    void replacePlaces_success_returnsOrderedPlaces() throws Exception {
        seedPlaces();
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("코스").type(CourseType.OFFICIAL).status(CourseStatus.DRAFT).isOrdered(true).build());

        String body = """
                [{"place_id": %d, "visit_order": 2}, {"place_id": %d, "visit_order": 1}]
                """.formatted(placeB.getId(), placeA.getId());

        mockMvc.perform(put("/admin/courses/{id}/places", course.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].place_id").value(placeA.getId()))
                .andExpect(jsonPath("$[0].visit_order").value(1))
                .andExpect(jsonPath("$[1].place_id").value(placeB.getId()));
    }

    @Test
    void approve_calledTwice_secondCallReturns409() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("검수 대상").type(CourseType.USER).status(CourseStatus.DRAFT).isOrdered(true).build());

        mockMvc.perform(post("/admin/courses/{id}/approve", course.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/courses/{id}/approve", course.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REVIEWED"));
    }

    @Test
    void reject_archivesTheCourse() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("검수 대상").type(CourseType.USER).status(CourseStatus.PUBLISHED).isOrdered(true).build());

        mockMvc.perform(post("/admin/courses/{id}/reject", course.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getStatus())
                .isEqualTo(CourseStatus.ARCHIVED);
    }

    @Test
    void pending_returnsAlreadyPublishedUnreviewedUserCourse() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("사용자 제작 코스").type(CourseType.USER).status(CourseStatus.PUBLISHED).isOrdered(true).build());

        mockMvc.perform(get("/admin/courses/pending").param("type", "USER")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(course.getId()));
    }

    @Test
    void delete_courseWithEnrollments_returns409WithCounts() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .organization(organization).name("참가자 있는 코스").type(CourseType.OFFICIAL).status(CourseStatus.PUBLISHED).isOrdered(true).build());
        User participant = userRepository.save(User.builder()
                .name("참가자").email("participant-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password1234")).role(Role.USER).build());
        courseEnrollmentRepository.save(CourseEnrollment.builder().course(course).user(participant).build());

        mockMvc.perform(delete("/admin/courses/{id}", course.getId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_HAS_ENROLLMENTS"))
                .andExpect(jsonPath("$.details[0].participants").value(1));
    }
}
