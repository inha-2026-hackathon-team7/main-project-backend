package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.user.StampRequest;
import com.hackathonteam7.mainprojectbackend.course.dto.user.StampResponse;
import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class StampController {

    private final StampService stampService;

    @Operation(summary = "QR 및 GPS 방문 인증")
    @PostMapping("/enrollments/{enrollmentId}/stamps")
    public StampResponse stamp(
            @AuthenticationPrincipal PrincipalUser me,
            @PathVariable @Positive Long enrollmentId,
            @Valid @RequestBody StampRequest request
    ) {
        return stampService.stamp(enrollmentId, me.userId(), request);
    }
}
