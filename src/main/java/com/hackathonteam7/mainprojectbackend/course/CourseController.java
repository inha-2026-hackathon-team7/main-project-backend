package com.hackathonteam7.mainprojectbackend.course;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/admin/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<CourseListItem> list(@AuthenticationPrincipal PrincipalUser me,
                                      @RequestParam(required = false) CourseType type,
                                      @RequestParam(required = false) CourseStatus status) {
        return courseService.list(me.orgId(), type, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetailResponse create(@AuthenticationPrincipal PrincipalUser me, @Valid @RequestBody CourseCreateRequest request) {
        return courseService.create(me.orgId(), request);
    }

    @GetMapping("/pending")
    public List<CoursePendingItem> pending(@AuthenticationPrincipal PrincipalUser me,
                                            @RequestParam(required = false) CourseType type) {
        return courseService.pending(me.orgId(), type);
    }

    @GetMapping("/{id}")
    public CourseDetailResponse getDetail(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id) {
        return courseService.getDetail(me.orgId(), id);
    }

    @PutMapping("/{id}")
    public CourseDetailResponse update(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id, @RequestBody JsonNode body) {
        CourseUpdateRequest request = objectMapper.convertValue(body, CourseUpdateRequest.class);
        JsonNullableField<Long> rewardIdPatch = body.has("reward_id")
                ? JsonNullableField.of(body.get("reward_id").isNull() ? null : body.get("reward_id").asLong())
                : JsonNullableField.absent();
        return courseService.update(me.orgId(), id, request, rewardIdPatch);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id) {
        courseService.delete(me.orgId(), id);
    }

    @PutMapping("/{id}/places")
    public List<CoursePlaceItem> replacePlaces(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id,
                                                @Valid @RequestBody List<CoursePlaceReplaceItem> items) {
        return courseService.replacePlaces(me.orgId(), id, items);
    }

    @GetMapping("/{id}/stats")
    public CourseStatsResponse stats(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id) {
        return courseService.stats(me.orgId(), id);
    }

    @PostMapping("/{id}/approve")
    public void approve(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id,
                         @RequestBody(required = false) CourseApproveRequest request) {
        courseService.approve(me.orgId(), id, me.userId(), request != null ? request : new CourseApproveRequest(null));
    }

    @PostMapping("/{id}/reject")
    public void reject(@AuthenticationPrincipal PrincipalUser me, @PathVariable Long id,
                        @RequestBody(required = false) CourseRejectRequest request) {
        courseService.reject(me.orgId(), id, me.userId(), request != null ? request : new CourseRejectRequest(null));
    }
}
