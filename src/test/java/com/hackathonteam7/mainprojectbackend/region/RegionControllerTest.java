package com.hackathonteam7.mainprojectbackend.region;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class RegionControllerTest extends IntegrationTestSupport {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void create_and_list_returnsCounts() throws Exception {
        String body = """
                {"name": "북촌", "type": "문화"}
                """;

        mockMvc.perform(post("/admin/regions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("북촌"))
                .andExpect(jsonPath("$.place_count").value(0))
                .andExpect(jsonPath("$.course_count").value(0));

        mockMvc.perform(get("/admin/regions").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("북촌"));
    }

    @Test
    void delete_regionWithPlaces_returns409WithPlaceDetails() throws Exception {
        Region region = regionRepository.save(Region.builder().organization(organization).name("성수").type("도심").build());
        placeRepository.save(Place.builder()
                .region(region)
                .organization(organization)
                .name("성수 카페")
                .latitude(new BigDecimal("37.5443"))
                .longitude(new BigDecimal("127.0557"))
                .qrcodeString(UUID.randomUUID().toString())
                .build());

        mockMvc.perform(delete("/admin/regions/{id}", region.getId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGION_HAS_PLACES"))
                .andExpect(jsonPath("$.details[0].name").value("성수 카페"));
    }

    @Test
    void delete_regionInAnotherOrganization_returns404() throws Exception {
        Organization otherOrg = organizationRepository.save(
                Organization.builder().name("다른 재단").type(OrganizationType.COMPANY).build());
        Region region = regionRepository.save(Region.builder().organization(otherOrg).name("남의 지역").build());

        mockMvc.perform(delete("/admin/regions/{id}", region.getId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REGION_NOT_FOUND"));
    }
}
