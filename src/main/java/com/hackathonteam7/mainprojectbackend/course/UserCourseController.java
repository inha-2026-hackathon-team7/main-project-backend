package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseDetailResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListQuery;
import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class UserCourseController {

    private final UserCourseQueryService userCourseQueryService;

    @Operation(summary = "공개 코스 목록 조회")
    @SecurityRequirements
    @GetMapping
    public List<UserCourseListItem> list(
            @RequestParam(name = "organization_id", required = false) @Positive Long organizationId,
            @RequestParam(name = "region_id", required = false) @Positive Long regionId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") BigDecimal lng
    ) {
        return userCourseQueryService.list(UserCourseListQuery.of(organizationId, regionId, type, page, size), lat, lng);
    }

    @Operation(summary = "공개 코스 상세 조회")
    @SecurityRequirements
    @GetMapping("/{courseId}")
    public UserCourseDetailResponse getDetail(
            @AuthenticationPrincipal PrincipalUser me,
            @PathVariable @Positive Long courseId,
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") BigDecimal lng
    ) {
        return userCourseQueryService.getDetail(courseId, me == null ? null : me.userId(), lat, lng);
    }
}
