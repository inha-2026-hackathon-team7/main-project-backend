package com.hackathonteam7.mainprojectbackend.course.policy;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GeoDistancePolicyTest {

    private final GeoDistancePolicy policy = new GeoDistancePolicy();

    @Test
    void acceptsSamePositionAndPositionInsideFiftyMeters() {
        assertDoesNotThrow(() -> policy.validate(
                new BigDecimal("0"), new BigDecimal("127"),
                new BigDecimal("0"), new BigDecimal("127")));
        assertDoesNotThrow(() -> policy.validate(
                new BigDecimal("0"), new BigDecimal("127"),
                new BigDecimal("0.0004400"), new BigDecimal("127")));
    }

    @Test
    void rejectsPositionOutsideFiftyMeters() {
        assertThatThrownBy(() -> policy.validate(
                new BigDecimal("0"), new BigDecimal("127"),
                new BigDecimal("0.0004600"), new BigDecimal("127")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.STAMP_OUT_OF_RANGE));
    }
}
