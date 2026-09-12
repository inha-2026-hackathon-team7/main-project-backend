package com.hackathonteam7.mainprojectbackend.course.policy;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class VisitOrderPolicy {

    public void validate(
            boolean ordered,
            Long targetCoursePlaceId,
            List<Long> orderedCoursePlaceIds,
            Set<Long> stampedCoursePlaceIds
    ) {
        if (!ordered) {
            return;
        }

        Long nextCoursePlaceId = orderedCoursePlaceIds.stream()
                .filter(id -> !stampedCoursePlaceIds.contains(id))
                .findFirst()
                .orElse(null);
        if (!targetCoursePlaceId.equals(nextCoursePlaceId)) {
            throw new ApiException(ErrorCode.STAMP_ORDER_VIOLATION);
        }
    }
}
