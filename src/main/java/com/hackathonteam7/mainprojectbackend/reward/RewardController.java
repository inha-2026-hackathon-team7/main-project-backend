package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.reward.dto.RewardCreateRequest;
import com.hackathonteam7.mainprojectbackend.reward.dto.RewardResponse;
import com.hackathonteam7.mainprojectbackend.reward.dto.RewardUpdateRequest;
import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @GetMapping
    public List<RewardResponse> list(@AuthenticationPrincipal PrincipalUser me) {
        return rewardService.list(me.orgId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RewardResponse create(@AuthenticationPrincipal PrincipalUser me, @Valid @RequestBody RewardCreateRequest request) {
        return rewardService.create(me.orgId(), request);
    }

    @PutMapping("/{id}")
    public RewardResponse update(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id,
                                  @Valid @RequestBody RewardUpdateRequest request) {
        return rewardService.update(me.orgId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id) {
        rewardService.delete(me.orgId(), id);
    }
}
