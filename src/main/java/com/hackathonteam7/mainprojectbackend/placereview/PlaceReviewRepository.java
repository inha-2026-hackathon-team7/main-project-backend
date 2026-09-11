package com.hackathonteam7.mainprojectbackend.placereview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {

    List<PlaceReview> findAllByPlaceIdOrderByCreatedAtDesc(Long placeId);
}
