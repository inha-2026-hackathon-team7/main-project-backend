package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.region.Region;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import com.hackathonteam7.mainprojectbackend.reward.Reward;
import com.hackathonteam7.mainprojectbackend.reward.RewardKind;
import com.hackathonteam7.mainprojectbackend.reward.RewardRepository;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserCourseIntegrationTest extends IntegrationTestSupport {

    @Autowired RegionRepository regionRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired RewardRepository rewardRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CoursePlaceRepository coursePlaceRepository;
    @Autowired CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    void listFiltersPublishedCoursesWithoutDuplicatesAndUsesFirstPlaceImage() throws Exception {
        Region region = region("목록 지역");
        Place first = place(region, "첫 장소", "first.jpg");
        Place second = place(region, "두 번째 장소", "second.jpg");
        Reward reward = reward();
        Course published = course("공개 공식 코스", CourseType.OFFICIAL, CourseStatus.PUBLISHED, reward, true);
        Course draft = course("비공개 코스", CourseType.OFFICIAL, CourseStatus.DRAFT, null, true);
        coursePlaceRepository.save(CoursePlace.builder().course(published).place(second).visitOrder(2).build());
        coursePlaceRepository.save(CoursePlace.builder().course(published).place(first).visitOrder(1).build());
        coursePlaceRepository.save(CoursePlace.builder().course(draft).place(first).visitOrder(1).build());

        var response = mockMvc.perform(get("/courses")
                        .queryParam("organization_id", organization.getId().toString())
                        .queryParam("region_id", region.getId().toString())
                        .queryParam("type", "official")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(published.getId()))
                .andExpect(jsonPath("$[0].thumbnail_url").value("first.jpg"))
                .andExpect(jsonPath("$[0].reward_summary.id").value(reward.getId()))
                .andExpect(jsonPath("$[0].reward_summary.kind").value("point"))
                .andReturn().getResponse();

        var item = objectMapper.readTree(response.getContentAsString()).get(0);
        assertThat(item.size()).isEqualTo(6);
        assertThat(item.get("reward_summary").size()).isEqualTo(4);
    }

    @Test
    void detailReturnsPlacesAndOnlyCurrentActiveEnrollmentAndIncrementsViews() throws Exception {
        Region region = region("상세 지역");
        Place place = place(region, "상세 장소", "detail.jpg");
        Reward reward = reward();
        Course course = course("상세 공개 코스", CourseType.USER, CourseStatus.PUBLISHED, reward, true);
        CoursePlace coursePlace = coursePlaceRepository.save(
                CoursePlace.builder().course(course).place(place).visitOrder(1).build());
        User participant = userRepository.save(User.builder()
                .name("일반 사용자")
                .email("course-user-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
        CourseEnrollment enrollment = courseEnrollmentRepository.save(
                CourseEnrollment.builder().course(course).user(participant).build());
        String userToken = jwtTokenProvider.generateAccessToken(
                participant.getId(), participant.getEmail(), participant.getRole(), null);

        mockMvc.perform(get("/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].course_place_id").value(coursePlace.getId()))
                .andExpect(jsonPath("$.places[0].lat").value(37.1234567))
                .andExpect(jsonPath("$.places[0].lng").value(127.1234567))
                .andExpect(jsonPath("$.reward.id").value(reward.getId()))
                .andExpect(jsonPath("$.my_enrollment_id").doesNotExist());

        mockMvc.perform(get("/courses/{id}", course.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.my_enrollment_id").value(enrollment.getId()));

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getViewCount()).isEqualTo(2);
    }

    @Test
    void repeatedViewByTheSameUserDoesNotDoubleCount() throws Exception {
        Region region = region("반복조회 지역");
        Place place = place(region, "반복조회 장소", "repeat.jpg");
        Course course = course("반복조회 코스", CourseType.OFFICIAL, CourseStatus.PUBLISHED, null, true);
        coursePlaceRepository.save(CoursePlace.builder().course(course).place(place).visitOrder(1).build());
        User participant = userRepository.save(User.builder()
                .name("반복 조회자")
                .email("repeat-view-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
        String userToken = jwtTokenProvider.generateAccessToken(
                participant.getId(), participant.getEmail(), participant.getRole(), null);

        mockMvc.perform(get("/courses/{id}", course.getId()).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/courses/{id}", course.getId()).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/courses/{id}", course.getId()).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getViewCount()).isEqualTo(1);
    }

    @Test
    void anonymousViewsAreCountedEveryTimeSinceTheyCannotBeDeduplicated() throws Exception {
        Region region = region("익명조회 지역");
        Place place = place(region, "익명조회 장소", "anon.jpg");
        Course course = course("익명조회 코스", CourseType.OFFICIAL, CourseStatus.PUBLISHED, null, true);
        coursePlaceRepository.save(CoursePlace.builder().course(course).place(place).visitOrder(1).build());

        mockMvc.perform(get("/courses/{id}", course.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/courses/{id}", course.getId())).andExpect(status().isOk());

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getViewCount()).isEqualTo(2);
    }

    @Test
    void draftCourseDetailIsNotExposedAndDoesNotIncreaseViews() throws Exception {
        Course draft = course("초안 코스", CourseType.AI, CourseStatus.DRAFT, null, false);

        mockMvc.perform(get("/courses/{id}", draft.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));

        assertThat(courseRepository.findById(draft.getId()).orElseThrow().getViewCount()).isZero();
    }

    private Region region(String name) {
        return regionRepository.save(Region.builder().organization(organization).name(name + UUID.randomUUID()).build());
    }

    private Place place(Region region, String name, String imageUrl) {
        return placeRepository.save(Place.builder()
                .region(region)
                .organization(organization)
                .name(name)
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .imageUrl(imageUrl)
                .qrcodeString(UUID.randomUUID().toString())
                .build());
    }

    private Reward reward() {
        return rewardRepository.save(Reward.builder()
                .organization(organization)
                .name("완주 포인트")
                .kind(RewardKind.POINT)
                .description("완주 보상")
                .imageUrl("reward.jpg")
                .stock(100)
                .validUntil(LocalDateTime.of(2026, 12, 31, 23, 59, 59))
                .build());
    }

    private Course course(
            String name, CourseType type, CourseStatus status, Reward reward, boolean ordered) {
        return courseRepository.save(Course.builder()
                .organization(organization)
                .name(name)
                .description("코스 설명")
                .type(type)
                .status(status)
                .reward(reward)
                .isOrdered(ordered)
                .build());
    }
}
