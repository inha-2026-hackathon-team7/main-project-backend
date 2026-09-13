package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.user.MyEnrollmentItem;
import com.hackathonteam7.mainprojectbackend.security.PrincipalUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/enrollments")
@RequiredArgsConstructor
public class MyEnrollmentController {

    private final MyEnrollmentQueryService myEnrollmentQueryService;

    @Operation(summary = "내 진행 중/완주 코스 목록 조회")
    @GetMapping
    public List<MyEnrollmentItem> list(
            @AuthenticationPrincipal PrincipalUser me,
            @RequestParam(required = false) String status
    ) {
        return myEnrollmentQueryService.list(me.userId(), status);
    }
}
