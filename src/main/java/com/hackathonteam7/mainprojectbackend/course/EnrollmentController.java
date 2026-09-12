package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentProgressResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentStartResponse;
import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentQueryService enrollmentQueryService;

    @Operation(summary = "코스 시작하기")
    @PostMapping("/courses/{courseId}/enrollments")
    public ResponseEntity<EnrollmentStartResponse> start(
            @AuthenticationPrincipal PrincipalUser me,
            @PathVariable @Positive Long courseId
    ) {
        EnrollmentService.EnrollmentStartResult result = enrollmentService.start(courseId, me.userId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @Operation(summary = "코스 진행 상황 조회")
    @GetMapping("/enrollments/{enrollmentId}")
    public EnrollmentProgressResponse getProgress(
            @AuthenticationPrincipal PrincipalUser me,
            @PathVariable @Positive Long enrollmentId
    ) {
        return enrollmentQueryService.getProgress(enrollmentId, me.userId());
    }
}
