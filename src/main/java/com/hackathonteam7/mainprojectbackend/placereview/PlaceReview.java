package com.hackathonteam7.mainprojectbackend.placereview;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.user.User;
import jakarta.persistence.Column;
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

/**
 * DB 테이블명은 "reviews" 다. course_reviews(관리자 코스 검수 이력, {@code review} 패키지)와는
 * 다른 개념 — 사용자가 장소에 남기는 후기 — 이므로 혼동을 피하기 위해 클래스명은 PlaceReview 로 둔다.
 */
@Getter
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uq_review_user_place", columnNames = {"user_id", "place_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private PlaceReview(User user, Place place, String content) {
        this.user = user;
        this.place = place;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
