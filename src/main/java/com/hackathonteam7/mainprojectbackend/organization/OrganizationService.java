package com.hackathonteam7.mainprojectbackend.organization;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.organization.dto.OrganizationListItem;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.place.dto.PublicPlaceItem;
import com.hackathonteam7.mainprojectbackend.region.RegionRepository;
import com.hackathonteam7.mainprojectbackend.region.dto.PublicRegionItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;

    public List<OrganizationListItem> list() {
        return organizationRepository.findAll().stream().map(OrganizationListItem::from).toList();
    }

    public List<PublicRegionItem> listRegions(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }
        return regionRepository.findPublicItemsByOrganizationId(organizationId);
    }

    public List<PublicPlaceItem> listPlaces(Long organizationId, Long regionId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }
        return placeRepository.findPublicItemsByOrganizationId(organizationId, regionId);
    }
}
