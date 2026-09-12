package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.course.dto.user.StampRequest;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.region.Region;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StampIntegrationTest extends IntegrationTestSupport {

    @Autowired RegionRepository regionRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CoursePlaceRepository coursePlaceRepository;
    @Autowired CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired CourseStampRepository courseStampRepository;
    @Autowired StampService stampService;

    private User participant;
    private String participantToken;
    private Course course;
    private Place firstPlace;
    private Place secondPlace;
    private CoursePlace firstCoursePlace;
    private CoursePlace secondCoursePlace;
    private CourseEnrollment enrollment;

    @BeforeEach
    void setUpStampFixture() {
        Region region = region(organization, "스탬프 지역");
        firstPlace = place(region, "첫 장소", "37.1234567", "127.1234567");
        secondPlace = place(region, "두 번째 장소", "37.1240000", "127.1240000");
        course = course(organization, "순서형 코스", true);
        firstCoursePlace = coursePlaceRepository.save(
                CoursePlace.builder().course(course).place(firstPlace).visitOrder(1).build());
        secondCoursePlace = coursePlaceRepository.save(
                CoursePlace.builder().course(course).place(secondPlace).visitOrder(2).build());

        participant = userRepository.save(User.builder()
                .name("스탬프 사용자")
                .email("stamp-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
        participantToken = jwtTokenProvider.generateAccessToken(
                participant.getId(), participant.getEmail(), participant.getRole(), null);
        enrollment = courseEnrollmentRepository.save(
                CourseEnrollment.builder().course(course).user(participant).build());
    }

    @Test
    void stampsPlacesAndCompletesEnrollmentAtLastPlace() throws Exception {
        stamp(firstPlace, "37.1234567", "127.1234567")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stamp_id").isNumber())
                .andExpect(jsonPath("$.place_name").value("첫 장소"))
                .andExpect(jsonPath("$.stamped_at").exists())
                .andExpect(jsonPath("$.progress.done").value(1))
                .andExpect(jsonPath("$.progress.total").value(2))
                .andExpect(jsonPath("$.course_completed").value(false));

        stamp(secondPlace, "37.1240000", "127.1240000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.done").value(2))
                .andExpect(jsonPath("$.progress.total").value(2))
                .andExpect(jsonPath("$.course_completed").value(true));

        CourseEnrollment completed = courseEnrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(CourseEnrollmentStatus.COMPLETE);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(courseStampRepository.countByCourseEnrollmentId(enrollment.getId())).isEqualTo(2);
    }

    @Test
    void rejectsUnknownQrPlaceOutsideCourseWrongOrderAndFarPosition() throws Exception {
        stamp("unknown-qr", "37.1234567", "127.1234567")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QR_PLACE_NOT_FOUND"));

        Region otherRegion = region(organization, "다른 지역");
        Place otherPlace = place(otherRegion, "다른 코스 장소", "37.1234567", "127.1234567");
        Course otherCourse = course(organization, "다른 코스", false);
        coursePlaceRepository.save(
                CoursePlace.builder().course(otherCourse).place(otherPlace).visitOrder(1).build());
        stamp(otherPlace, "37.1234567", "127.1234567")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("PLACE_NOT_IN_COURSE"));

        stamp(secondPlace, "37.1240000", "127.1240000")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("STAMP_ORDER_VIOLATION"));

        stamp(firstPlace, "37.1300000", "127.1300000")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("STAMP_OUT_OF_RANGE"));

        assertThat(courseStampRepository.countByCourseEnrollmentId(enrollment.getId())).isZero();
    }

    @Test
    void rejectsDuplicateStampAndForeignEnrollment() throws Exception {
        stamp(firstPlace, "37.1234567", "127.1234567")
                .andExpect(status().isOk());
        stamp(firstPlace, "37.1234567", "127.1234567")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STAMP_ALREADY_EXISTS"));

        User otherUser = userRepository.save(User.builder()
                .name("다른 사용자")
                .email("other-stamp-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
        String otherToken = jwtTokenProvider.generateAccessToken(
                otherUser.getId(), otherUser.getEmail(), otherUser.getRole(), null);
        mockMvc.perform(post("/enrollments/{id}/stamps", enrollment.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(firstPlace, "37.1234567", "127.1234567")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_FOUND"));

        assertThat(courseStampRepository.countByCourseEnrollmentId(enrollment.getId())).isEqualTo(1);
    }

    @Test
    void validatesRequestAndRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/enrollments/{id}/stamps", enrollment.getId())
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qrcode_string":"", "latitude":91, "longitude":181}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/enrollments/{id}/stamps", enrollment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(firstPlace, "37.1234567", "127.1234567")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void serializesConcurrentScansForSameEnrollmentAndPlace() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> concurrentStamp(ready, start));
            Future<String> second = executor.submit(() -> concurrentStamp(ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "STAMP_ALREADY_EXISTS");
            assertThat(courseStampRepository.countByCourseEnrollmentId(enrollment.getId())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private String concurrentStamp(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            stampService.stamp(
                    enrollment.getId(),
                    participant.getId(),
                    new StampRequest(
                            firstPlace.getQrcodeString(),
                            firstPlace.getLatitude(),
                            firstPlace.getLongitude()));
            return "SUCCESS";
        } catch (ApiException exception) {
            return exception.getErrorCode().name();
        }
    }

    private org.springframework.test.web.servlet.ResultActions stamp(
            Place place,
            String latitude,
            String longitude
    ) throws Exception {
        return stamp(place.getQrcodeString(), latitude, longitude);
    }

    private org.springframework.test.web.servlet.ResultActions stamp(
            String qrcodeString,
            String latitude,
            String longitude
    ) throws Exception {
        return mockMvc.perform(post("/enrollments/{id}/stamps", enrollment.getId())
                .header("Authorization", "Bearer " + participantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(qrcodeString, latitude, longitude)));
    }

    private String body(Place place, String latitude, String longitude) {
        return body(place.getQrcodeString(), latitude, longitude);
    }

    private String body(String qrcodeString, String latitude, String longitude) {
        return """
                {
                  "qrcode_string": "%s",
                  "latitude": %s,
                  "longitude": %s
                }
                """.formatted(qrcodeString, latitude, longitude);
    }

    private Region region(Organization owner, String name) {
        return regionRepository.save(Region.builder()
                .organization(owner)
                .name(name + "-" + UUID.randomUUID())
                .build());
    }

    private Place place(Region region, String name, String latitude, String longitude) {
        return placeRepository.save(Place.builder()
                .region(region)
                .organization(region.getOrganization())
                .name(name)
                .latitude(new BigDecimal(latitude))
                .longitude(new BigDecimal(longitude))
                .qrcodeString(UUID.randomUUID().toString())
                .build());
    }

    private Course course(Organization owner, String name, boolean ordered) {
        return courseRepository.save(Course.builder()
                .organization(owner)
                .name(name)
                .description("스탬프 테스트 코스")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(ordered)
                .build());
    }
}
