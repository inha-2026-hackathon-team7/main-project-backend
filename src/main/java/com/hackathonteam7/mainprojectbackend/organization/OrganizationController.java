package com.hackathonteam7.mainprojectbackend.organization;

import com.hackathonteam7.mainprojectbackend.organization.dto.OrganizationListItem;
import com.hackathonteam7.mainprojectbackend.place.dto.PublicPlaceItem;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "조직 목록 조회")
    @GetMapping
    public List<OrganizationListItem> list() {
        return organizationService.list();
    }

    @Operation(summary = "조직의 장소 목록 조회")
    @GetMapping("/{organizationId}/places")
    public List<PublicPlaceItem> listPlaces(@PathVariable @Positive Long organizationId) {
        return organizationService.listPlaces(organizationId);
    }
}
