package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseCreateRequest;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseDetailResponse;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCourseCommandService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final OrganizationRepository organizationRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserCourseDetailResponse create(Long userId, UserCourseCreateRequest request) {
        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_FOUND));

        List<Long> placeIds = request.placeIds();
        Map<Long, Place> ownedPlaces = placeRepository.findAllById(placeIds).stream()
                .filter(place -> place.getOrganization().getId().equals(request.organizationId()))
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        if (ownedPlaces.size() != new HashSet<>(placeIds).size()) {
            throw new ApiException(ErrorCode.PLACE_NOT_FOUND);
        }
        if (new HashSet<>(placeIds).size() != placeIds.size()) {
            throw new ApiException(ErrorCode.DUPLICATE_PLACE);
        }

        Course course = courseRepository.save(
                Course.builder()
                        .organization(organization)
                        .creator(userRepository.getReferenceById(userId))
                        .name(request.name())
                        .description(request.description())
                        .type(CourseType.USER)
                        .status(CourseStatus.PUBLISHED)
                        .isOrdered(true)
                        .build()
        );

        for (int i = 0; i < placeIds.size(); i++) {
            coursePlaceRepository.save(
                    CoursePlace.builder()
                            .course(course)
                            .place(ownedPlaces.get(placeIds.get(i)))
                            .visitOrder(i + 1)
                            .build()
            );
        }

        return UserCourseDetailResponse.from(
                course, coursePlaceRepository.findUserPlaceItemsByCourseId(course.getId()),
                null, null, null, null);
    }
}
