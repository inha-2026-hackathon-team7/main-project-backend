package com.hackathonteam7.mainprojectbackend.placereview;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceReviewLikeRepository extends JpaRepository<PlaceReviewLike, Long> {

    long countByReviewId(Long reviewId);

    Optional<PlaceReviewLike> findByUserIdAndReviewId(Long userId, Long reviewId);
}
