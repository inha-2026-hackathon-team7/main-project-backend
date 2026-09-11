package com.hackathonteam7.mainprojectbackend.review;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseCreatedAtEntity;
import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.reward.Reward;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReview extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    private User reviewer;

    @Column(nullable = false, length = 20)
    private CourseReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bonus_reward_id")
    private Reward bonusReward;

    @Builder
    private CourseReview(Course course, User reviewer, CourseReviewDecision decision, String reason, Reward bonusReward) {
        this.course = course;
        this.reviewer = reviewer;
        this.decision = decision;
        this.reason = reason;
        this.bonusReward = bonusReward;
    }
}
