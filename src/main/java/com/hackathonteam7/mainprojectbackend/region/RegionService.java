package com.hackathonteam7.mainprojectbackend.region;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorDetail;
import com.hackathonteam7.mainprojectbackend.course.CoursePlaceRepository;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.region.dto.RegionCreateRequest;
import com.hackathonteam7.mainprojectbackend.region.dto.RegionSummary;
import com.hackathonteam7.mainprojectbackend.region.dto.RegionUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;
    private final PlaceRepository placeRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final OrganizationRepository organizationRepository;

    public List<RegionSummary> list(Long orgId) {
        return regionRepository.findSummaries(orgId);
    }

    @Transactional
    public RegionSummary create(Long orgId, RegionCreateRequest request) {
        Organization organization = organizationRepository.getReferenceById(orgId);
        Region region = regionRepository.save(
                Region.builder()
                        .organization(organization)
                        .name(request.name())
                        .type(request.type())
                        .build()
        );
        return new RegionSummary(region.getId(), region.getName(), region.getType(), 0L, 0L);
    }

    @Transactional
    public RegionSummary update(Long orgId, Long id, RegionUpdateRequest request) {
        Region region = getOwnedRegion(orgId, id);
        String name = request.name() != null ? request.name() : region.getName();
        String type = request.type() != null ? request.type() : region.getType();
        region.rename(name, type);

        long placeCount = placeRepository.countByRegionId(id);
        long courseCount = coursePlaceRepository.countDistinctCoursesByRegionId(id);
        return new RegionSummary(region.getId(), region.getName(), region.getType(), placeCount, courseCount);
    }

    @Transactional
    public void delete(Long orgId, Long id) {
        Region region = getOwnedRegion(orgId, id);

        List<Place> places = placeRepository.findAllByRegionId(id);
        if (!places.isEmpty()) {
            List<ErrorDetail> details = places.stream()
                    .map(place -> new ErrorDetail(place.getId(), place.getName(), place.getCategory()))
                    .toList();
            throw new ApiException(ErrorCode.REGION_HAS_PLACES, details);
        }

        regionRepository.delete(region);
    }

    private Region getOwnedRegion(Long orgId, Long id) {
        return regionRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
    }
}
