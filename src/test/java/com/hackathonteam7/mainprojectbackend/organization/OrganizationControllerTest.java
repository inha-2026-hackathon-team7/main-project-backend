package com.hackathonteam7.mainprojectbackend.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.region.Region;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrganizationControllerTest extends IntegrationTestSupport {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Test
    void list_returnsOrganizationsWithLowercaseType() throws Exception {
        mockMvc.perform(get("/organizations").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("facility"));
    }

    @Test
    void listPlaces_excludesQrcodeStringAndReturnsRegionInfo() throws Exception {
        Region region = regionRepository.save(Region.builder().organization(organization).name("성수").build());
        Place place = placeRepository.save(Place.builder()
                .region(region).organization(organization).name("장소")
                .latitude(new BigDecimal("37.1")).longitude(new BigDecimal("127.1"))
                .qrcodeString(UUID.randomUUID().toString()).build());

        mockMvc.perform(get("/organizations/{id}/places", organization.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(place.getId()))
                .andExpect(jsonPath("$[0].region_id").value(region.getId()))
                .andExpect(jsonPath("$[0].region_name").value("성수"))
                .andExpect(jsonPath("$[0].qrcode_string").doesNotExist());
    }

    @Test
    void listPlaces_unknownOrganization_returns404() throws Exception {
        mockMvc.perform(get("/organizations/{id}/places", 999_999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void listRegions_returnsRegionsWithPlaceCount() throws Exception {
        Region region = regionRepository.save(Region.builder().organization(organization).name("성수").type("도심").build());
        placeRepository.save(Place.builder()
                .region(region).organization(organization).name("장소")
                .latitude(new BigDecimal("37.1")).longitude(new BigDecimal("127.1"))
                .qrcodeString(UUID.randomUUID().toString()).build());

        mockMvc.perform(get("/organizations/{id}/regions", organization.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(region.getId()))
                .andExpect(jsonPath("$[0].name").value("성수"))
                .andExpect(jsonPath("$[0].type").value("도심"))
                .andExpect(jsonPath("$[0].place_count").value(1));
    }

    @Test
    void listRegions_unknownOrganization_returns404() throws Exception {
        mockMvc.perform(get("/organizations/{id}/regions", 999_999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void listPlaces_filteredByRegion_onlyReturnsThatRegionsPlaces() throws Exception {
        Region regionA = regionRepository.save(Region.builder().organization(organization).name("성수").build());
        Region regionB = regionRepository.save(Region.builder().organization(organization).name("홍대").build());
        Place placeA = placeRepository.save(Place.builder()
                .region(regionA).organization(organization).name("성수 장소")
                .latitude(new BigDecimal("37.1")).longitude(new BigDecimal("127.1"))
                .qrcodeString(UUID.randomUUID().toString()).build());
        placeRepository.save(Place.builder()
                .region(regionB).organization(organization).name("홍대 장소")
                .latitude(new BigDecimal("37.2")).longitude(new BigDecimal("127.2"))
                .qrcodeString(UUID.randomUUID().toString()).build());

        mockMvc.perform(get("/organizations/{id}/places", organization.getId())
                        .param("region_id", String.valueOf(regionA.getId()))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(placeA.getId()));
    }
}
