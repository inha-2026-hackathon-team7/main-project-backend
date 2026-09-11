package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorDetail;
import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CourseRepository;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.reward.dto.RewardCreateRequest;
import com.hackathonteam7.mainprojectbackend.reward.dto.RewardResponse;
import com.hackathonteam7.mainprojectbackend.reward.dto.RewardUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardService {

    private final RewardRepository rewardRepository;
    private final CourseRepository courseRepository;
    private final OrganizationRepository organizationRepository;

    public List<RewardResponse> list(Long orgId) {
        return rewardRepository.findAllByOrganizationIdOrderById(orgId).stream()
                .map(reward -> RewardResponse.from(reward, courseRepository.countByRewardId(reward.getId())))
                .toList();
    }

    @Transactional
    public RewardResponse create(Long orgId, RewardCreateRequest request) {
        Organization organization = organizationRepository.getReferenceById(orgId);
        Reward reward = rewardRepository.save(
                Reward.builder()
                        .organization(organization)
                        .name(request.name())
                        .kind(request.kind())
                        .description(request.description())
                        .imageUrl(request.imageUrl())
                        .stock(request.stock())
                        .validUntil(request.validUntil())
                        .build()
        );
        return RewardResponse.from(reward, 0L);
    }

    @Transactional
    public RewardResponse update(Long orgId, Long id, RewardUpdateRequest request) {
        Reward reward = getOwned(orgId, id);
        if (request.name() != null) {
            reward.rename(request.name());
        }
        if (request.stock() != null) {
            reward.changeStock(request.stock());
        }
        if (request.validUntil() != null) {
            reward.changeValidUntil(request.validUntil());
        }
        return RewardResponse.from(reward, courseRepository.countByRewardId(id));
    }

    @Transactional
    public void delete(Long orgId, Long id) {
        Reward reward = getOwned(orgId, id);
        List<Course> linkedCourses = courseRepository.findAllByRewardId(id);
        if (!linkedCourses.isEmpty()) {
            List<ErrorDetail> details = linkedCourses.stream()
                    .map(course -> new ErrorDetail(course.getId(), course.getName(),
                            "status=" + course.getStatus().name().toLowerCase()))
                    .toList();
            throw new ApiException(ErrorCode.REWARD_IN_USE, details);
        }
        rewardRepository.delete(reward);
    }

    private Reward getOwned(Long orgId, Long id) {
        return rewardRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.REWARD_NOT_FOUND));
    }
}
