package com.hackathonteam7.mainprojectbackend.course.policy;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class GeoDistancePolicy {

    static final double EARTH_RADIUS_METERS = 6_371_000.0;
    static final double ALLOWED_RADIUS_METERS = 50.0;

    public void validate(
            BigDecimal placeLatitude,
            BigDecimal placeLongitude,
            BigDecimal requestLatitude,
            BigDecimal requestLongitude
    ) {
        if (distanceMeters(placeLatitude, placeLongitude, requestLatitude, requestLongitude)
                > ALLOWED_RADIUS_METERS) {
            throw new ApiException(ErrorCode.STAMP_OUT_OF_RANGE);
        }
    }

    public double distanceMeters(
            BigDecimal firstLatitude,
            BigDecimal firstLongitude,
            BigDecimal secondLatitude,
            BigDecimal secondLongitude
    ) {
        double lat1 = Math.toRadians(firstLatitude.doubleValue());
        double lat2 = Math.toRadians(secondLatitude.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(secondLongitude.doubleValue() - firstLongitude.doubleValue());

        double sinLat = Math.sin(deltaLat / 2.0);
        double sinLon = Math.sin(deltaLon / 2.0);
        double a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double clamped = Math.max(0.0, Math.min(1.0, a));
        return 2.0 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(clamped));
    }
}
