package com.hackathonteam7.mainprojectbackend.course.policy;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class VisitOrderPolicyTest {

    private final VisitOrderPolicy policy = new VisitOrderPolicy();

    @Test
    void orderedCourseAcceptsOnlyFirstUnstampedPlace() {
        assertDoesNotThrow(() -> policy.validate(true, 2L, List.of(1L, 2L, 3L), Set.of(1L)));

        assertThatThrownBy(() -> policy.validate(true, 3L, List.of(1L, 2L, 3L), Set.of(1L)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.STAMP_ORDER_VIOLATION));
    }

    @Test
    void unorderedCourseAcceptsAnyUnstampedPlace() {
        assertDoesNotThrow(() -> policy.validate(false, 3L, List.of(1L, 2L, 3L), Set.of()));
    }
}
