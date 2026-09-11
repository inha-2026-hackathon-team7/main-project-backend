package com.hackathonteam7.mainprojectbackend.placereview;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseCreatedAtEntity;
import com.hackathonteam7.mainprojectbackend.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "review_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_review_like", columnNames = {"user_id", "review_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReviewLike extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private PlaceReview review;

    @Builder
    private PlaceReviewLike(User user, PlaceReview review) {
        this.user = user;
        this.review = review;
    }
}
