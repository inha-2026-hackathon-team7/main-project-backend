package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.reward.dto.user.RewardClaimRequest;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.RewardClaimResponse;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.UserRewardClaimItem;
import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class RewardClaimController {

    private final RewardClaimService rewardClaimService;
    private final RewardClaimQueryService rewardClaimQueryService;

    @Operation(summary = "코스 완주 리워드 수령")
    @PostMapping("/reward-claims")
    public ResponseEntity<RewardClaimResponse> claim(
            @AuthenticationPrincipal PrincipalUser me,
            @Valid @RequestBody RewardClaimRequest request
    ) {
        RewardClaimService.RewardClaimResult result = rewardClaimService.claim(me.userId(), request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @Operation(summary = "리워드 사용 처리(교환 완료)")
    @PostMapping("/reward-claims/{claimId}/redeem")
    public RewardClaimResponse redeem(
            @AuthenticationPrincipal PrincipalUser me,
            @PathVariable @Positive Long claimId
    ) {
        return rewardClaimService.redeem(me.userId(), claimId);
    }

    @Operation(summary = "내 리워드함 조회")
    @GetMapping("/users/me/reward-claims")
    public List<UserRewardClaimItem> list(@AuthenticationPrincipal PrincipalUser me) {
        return rewardClaimQueryService.list(me.userId());
    }
}
