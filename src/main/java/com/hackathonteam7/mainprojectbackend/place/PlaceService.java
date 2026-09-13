package com.hackathonteam7.mainprojectbackend.place;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorDetail;
import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CoursePlaceRepository;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.place.dto.PlaceCreateRequest;
import com.hackathonteam7.mainprojectbackend.place.dto.PlaceDetailResponse;
import com.hackathonteam7.mainprojectbackend.place.dto.PlaceResponse;
import com.hackathonteam7.mainprojectbackend.place.dto.PlaceUpdateRequest;
import com.hackathonteam7.mainprojectbackend.region.Region;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;
    private final OrganizationRepository organizationRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final QrCodeService qrCodeService;

    public List<PlaceResponse> list(Long orgId, Long regionId) {
        List<Place> places = regionId != null
                ? placeRepository.findAllByOrganizationIdAndRegionIdOrderById(orgId, regionId)
                : placeRepository.findAllByOrganizationIdOrderById(orgId);
        return places.stream().map(PlaceResponse::from).toList();
    }

    @Transactional
    public PlaceResponse create(Long orgId, PlaceCreateRequest request) {
        Region region = regionRepository.findByIdAndOrganizationId(request.regionId(), orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        Organization organization = organizationRepository.getReferenceById(orgId);

        Place place = placeRepository.save(
                Place.builder()
                        .region(region)
                        .organization(organization)
                        .name(request.name())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .category(request.category())
                        .description(request.description())
                        .imageUrl(request.imageUrl())
                        .qrcodeString(UUID.randomUUID().toString())
                        .build()
        );
        return PlaceResponse.from(place);
    }

    public PlaceDetailResponse getDetail(Long orgId, Long id) {
        Place place = getOwned(orgId, id);
        List<Course> referencingCourses = coursePlaceRepository.findDistinctCoursesByPlaceId(id);
        return PlaceDetailResponse.from(place, referencingCourses);
    }

    @Transactional
    public PlaceResponse update(Long orgId, Long id, PlaceUpdateRequest request) {
        Place place = getOwned(orgId, id);
        place.update(request.name(), request.latitude(), request.longitude(), request.category(),
                request.description(), request.imageUrl());
        return PlaceResponse.from(place);
    }

    @Transactional
    public void delete(Long orgId, Long id) {
        Place place = getOwned(orgId, id);
        List<Course> referencingCourses = coursePlaceRepository.findDistinctCoursesByPlaceId(id);
        if (!referencingCourses.isEmpty()) {
            List<ErrorDetail> details = referencingCourses.stream()
                    .map(course -> new ErrorDetail(course.getId(), course.getName(),
                            "status=" + course.getStatus().name().toLowerCase()))
                    .toList();
            throw new ApiException(ErrorCode.PLACE_IN_USE, details);
        }
        placeRepository.delete(place);
    }

    public byte[] qrcodePng(Long orgId, Long id) {
        Place place = getOwned(orgId, id);
        return qrCodeService.generatePng(place.getQrcodeString());
    }

    private Place getOwned(Long orgId, Long id) {
        return placeRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.PLACE_NOT_FOUND));
    }
}
