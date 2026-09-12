package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.common.web.JsonNullableField;
import com.hackathonteam7.mainprojectbackend.course.dto.CourseApproveRequest;
import com.hackathonteam7.mainprojectbackend.course.dto.CourseCreateRequest;
import com.hackathonteam7.mainprojectbackend.course.dto.CourseDetailResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.CourseListItem;
import com.hackathonteam7.mainprojectbackend.course.dto.CoursePendingItem;
import com.hackathonteam7.mainprojectbackend.course.dto.CoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.CoursePlaceReplaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.CourseRejectRequest;
import com.hackathonteam7.mainprojectbackend.course.dto.CourseStatsResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.CourseUpdateRequest;
import com.hackathonteam7.mainprojectbackend.course.dto.EnrollmentCountsDetail;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.place.PlaceRepository;
import com.hackathonteam7.mainprojectbackend.review.CourseReview;
import com.hackathonteam7.mainprojectbackend.review.CourseReviewDecision;
import com.hackathonteam7.mainprojectbackend.review.CourseReviewRepository;
import com.hackathonteam7.mainprojectbackend.reward.Reward;
import com.hackathonteam7.mainprojectbackend.reward.RewardClaimRepository;
import com.hackathonteam7.mainprojectbackend.reward.RewardRepository;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import jakarta.persistence.EntityManager;
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
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final RewardClaimRepository rewardClaimRepository;
    private final RewardRepository rewardRepository;
    private final PlaceRepository placeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final EntityManager entityManager;

    public List<CourseListItem> list(Long orgId, CourseType type, CourseStatus status) {
        return courseRepository.findListRows(orgId, type, status).stream()
                .map(com.hackathonteam7.mainprojectbackend.course.dto.CourseListRow::toItem)
                .toList();
    }

    @Transactional
    public CourseDetailResponse create(Long orgId, CourseCreateRequest request) {
        Organization organization = organizationRepository.getReferenceById(orgId);
        Reward reward = request.rewardId() != null ? getOwnedReward(orgId, request.rewardId()) : null;

        Course course = courseRepository.save(
                Course.builder()
                        .organization(organization)
                        .reward(reward)
                        .name(request.name())
                        .description(request.description())
                        .type(CourseType.OFFICIAL)
                        .status(CourseStatus.DRAFT)
                        .isOrdered(request.isOrdered())
                        .build()
        );
        return CourseDetailResponse.from(course, List.of());
    }

    public CourseDetailResponse getDetail(Long orgId, Long id) {
        Course course = getOwned(orgId, id);
        return CourseDetailResponse.from(course, coursePlaceRepository.findPlaceItemsByCourseId(id));
    }

    @Transactional
    public CourseDetailResponse update(Long orgId, Long id, CourseUpdateRequest request,
                                        JsonNullableField<Long> rewardIdPatch) {
        Course course = getOwnedForUpdate(orgId, id);
        boolean changesStructure = request.isOrdered() != null || rewardIdPatch.present();
        if (changesStructure && courseEnrollmentRepository.countByCourseId(id) > 0) {
            throw new ApiException(ErrorCode.COURSE_STRUCTURE_LOCKED);
        }

        if (request.name() != null || request.description() != null) {
            String name = request.name() != null ? request.name() : course.getName();
            String description = request.description() != null ? request.description() : course.getDescription();
            course.updateDetails(name, description);
        }
        if (request.isOrdered() != null) {
            course.changeIsOrdered(request.isOrdered());
        }
        if (request.status() != null) {
            course.changeStatus(request.status());
        }
        if (rewardIdPatch.present()) {
            Reward reward = rewardIdPatch.value() != null ? getOwnedReward(orgId, rewardIdPatch.value()) : null;
            course.changeReward(reward);
        }

        return CourseDetailResponse.from(course, coursePlaceRepository.findPlaceItemsByCourseId(id));
    }

    @Transactional
    public void delete(Long orgId, Long id) {
        Course course = getOwned(orgId, id);
        long[] statusCounts = computeStatusCounts(id);
        long participants = statusCounts[0];
        if (participants > 0) {
            long rewardClaimed = rewardClaimRepository.countByCourseEnrollmentCourseId(id);
            throw new ApiException(ErrorCode.COURSE_HAS_ENROLLMENTS,
                    List.of(new EnrollmentCountsDetail(participants, statusCounts[1], rewardClaimed)));
        }
        courseRepository.delete(course);
    }

    @Transactional
    public List<CoursePlaceItem> replacePlaces(Long orgId, Long id, List<CoursePlaceReplaceItem> items) {
        Course course = getOwnedForUpdate(orgId, id);
        if (courseEnrollmentRepository.countByCourseId(id) > 0) {
            throw new ApiException(ErrorCode.COURSE_STRUCTURE_LOCKED);
        }

        List<Long> placeIds = items.stream().map(CoursePlaceReplaceItem::placeId).toList();

        Map<Long, Place> ownedPlaces = placeIds.isEmpty() ? Map.of() : placeRepository.findAllById(placeIds).stream()
                .filter(place -> place.getOrganization().getId().equals(orgId))
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        if (ownedPlaces.size() != new HashSet<>(placeIds).size()) {
            throw new ApiException(ErrorCode.PLACE_NOT_FOUND);
        }

        List<Integer> sortedOrders = items.stream().map(CoursePlaceReplaceItem::visitOrder).sorted().toList();
        for (int i = 0; i < sortedOrders.size(); i++) {
            if (!sortedOrders.get(i).equals(i + 1)) {
                throw new ApiException(ErrorCode.INVALID_VISIT_ORDER);
            }
        }

        if (new HashSet<>(placeIds).size() != placeIds.size()) {
            throw new ApiException(ErrorCode.DUPLICATE_PLACE);
        }

        coursePlaceRepository.deleteAllByCourseId(id);
        entityManager.flush();

        for (CoursePlaceReplaceItem item : items) {
            coursePlaceRepository.save(
                    CoursePlace.builder()
                            .course(course)
                            .place(ownedPlaces.get(item.placeId()))
                            .visitOrder(item.visitOrder())
                            .build()
            );
        }

        return coursePlaceRepository.findPlaceItemsByCourseId(id);
    }

    public CourseStatsResponse stats(Long orgId, Long id) {
        Course course = getOwned(orgId, id);
        long[] statusCounts = computeStatusCounts(id);
        long rewardClaimed = rewardClaimRepository.countByCourseEnrollmentCourseId(id);
        return new CourseStatsResponse(course.getViewCount(), statusCounts[0], statusCounts[1], statusCounts[2], rewardClaimed);
    }

    public List<CoursePendingItem> pending(Long orgId, CourseType type) {
        List<CourseType> types = type != null ? List.of(type) : List.of(CourseType.USER, CourseType.AI);
        return courseRepository.findAllByOrganizationIdAndStatusAndTypeInOrderByCreatedAtDesc(orgId, CourseStatus.DRAFT, types)
                .stream()
                .map(course -> new CoursePendingItem(
                        course.getId(),
                        course.getName(),
                        course.getCreator() != null ? course.getCreator().getName() : null,
                        course.getCreatedAt(),
                        coursePlaceRepository.countByCourseId(course.getId()),
                        null
                ))
                .toList();
    }

    @Transactional
    public void approve(Long orgId, Long id, Long reviewerUserId, CourseApproveRequest request) {
        Course course = getOwned(orgId, id);
        ensureNotAlreadyReviewed(id);

        Reward bonusReward = request.bonusRewardId() != null ? getOwnedReward(orgId, request.bonusRewardId()) : null;
        course.approve(bonusReward);

        courseReviewRepository.save(
                CourseReview.builder()
                        .course(course)
                        .reviewer(userRepository.getReferenceById(reviewerUserId))
                        .decision(CourseReviewDecision.APPROVED)
                        .bonusReward(bonusReward)
                        .build()
        );
    }

    @Transactional
    public void reject(Long orgId, Long id, Long reviewerUserId, CourseRejectRequest request) {
        Course course = getOwned(orgId, id);
        ensureNotAlreadyReviewed(id);

        courseReviewRepository.save(
                CourseReview.builder()
                        .course(course)
                        .reviewer(userRepository.getReferenceById(reviewerUserId))
                        .decision(CourseReviewDecision.REJECTED)
                        .reason(request.reason())
                        .build()
        );
    }

    private void ensureNotAlreadyReviewed(Long courseId) {
        if (courseReviewRepository.existsByCourseId(courseId)) {
            throw new ApiException(ErrorCode.ALREADY_REVIEWED);
        }
    }

    /** [participants, completed, abandoned] — CourseEnrollmentRepository#countGroupByStatus 단일 쿼리로 계산. */
    private long[] computeStatusCounts(Long courseId) {
        long completed = 0;
        long abandoned = 0;
        long active = 0;
        for (Object[] row : courseEnrollmentRepository.countGroupByStatus(courseId)) {
            CourseEnrollmentStatus status = (CourseEnrollmentStatus) row[0];
            long count = (Long) row[1];
            switch (status) {
                case COMPLETE -> completed = count;
                case ABANDONED -> abandoned = count;
                case ACTIVE -> active = count;
            }
        }
        return new long[]{completed + abandoned + active, completed, abandoned};
    }

    private Reward getOwnedReward(Long orgId, Long rewardId) {
        return rewardRepository.findByIdAndOrganizationId(rewardId, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.REWARD_NOT_FOUND));
    }

    private Course getOwned(Long orgId, Long id) {
        return courseRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
    }

    private Course getOwnedForUpdate(Long orgId, Long id) {
        return courseRepository.findByIdAndOrganizationIdForUpdate(id, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
    }
}
