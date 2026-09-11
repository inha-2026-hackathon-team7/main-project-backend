package com.hackathonteam7.mainprojectbackend.place;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CoursePlace;
import com.hackathonteam7.mainprojectbackend.course.CoursePlaceRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseType;
import com.hackathonteam7.mainprojectbackend.region.Region;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PlaceControllerTest extends IntegrationTestSupport {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CoursePlaceRepository coursePlaceRepository;

    private Region region;

    @Test
    void create_serverGeneratesQrcode_ignoringRequestValue() throws Exception {
        region = regionRepository.save(Region.builder().organization(organization).name("성수").build());
        String body = """
                {
                  "name": "성수 카페",
                  "region_id": %d,
                  "latitude": 37.5443,
                  "longitude": 127.0557,
                  "qrcode_string": "client-supplied-value-should-be-ignored"
                }
                """.formatted(region.getId());

        mockMvc.perform(post("/admin/places")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrcode_string").value(org.hamcrest.Matchers.not("client-supplied-value-should-be-ignored")));
    }

    @Test
    void delete_placeReferencedByCourse_returns409WithCourseDetails() throws Exception {
        region = regionRepository.save(Region.builder().organization(organization).name("성수").build());
        Place place = placeRepository.save(Place.builder()
                .region(region)
                .organization(organization)
                .name("성수 카페")
                .latitude(new BigDecimal("37.5443"))
                .longitude(new BigDecimal("127.0557"))
                .qrcodeString(UUID.randomUUID().toString())
                .build());
        Course course = courseRepository.save(Course.builder()
                .organization(organization)
                .name("성수 투어")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(true)
                .build());
        coursePlaceRepository.save(CoursePlace.builder().course(course).place(place).visitOrder(1).build());

        mockMvc.perform(delete("/admin/places/{id}", place.getId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLACE_IN_USE"))
                .andExpect(jsonPath("$.details[0].name").value("성수 투어"))
                .andExpect(jsonPath("$.details[0].note").value("status=published"));
    }

    @Test
    void getDetail_returnsReferencingCourses() throws Exception {
        region = regionRepository.save(Region.builder().organization(organization).name("성수").build());
        Place place = placeRepository.save(Place.builder()
                .region(region)
                .organization(organization)
                .name("성수 카페")
                .latitude(new BigDecimal("37.5443"))
                .longitude(new BigDecimal("127.0557"))
                .qrcodeString(UUID.randomUUID().toString())
                .build());

        mockMvc.perform(get("/admin/places/{id}", place.getId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referencing_courses").isEmpty());
    }
}
