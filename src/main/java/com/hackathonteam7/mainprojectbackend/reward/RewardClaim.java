package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.course.CourseEnrollment;
import com.hackathonteam7.mainprojectbackend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reward_claims")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_enrollment_id", nullable = false)
    private CourseEnrollment courseEnrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    private Reward reward;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /** DB 에 값 목록을 제약하는 CHECK 가 없어 자유 문자열로 둔다 (기본값 "claimed"). */
    @Column(nullable = false, length = 20)
    private String status;

    @CreatedDate
    @Column(name = "claimed_at", nullable = false, updatable = false)
    private LocalDateTime claimedAt;

    @Builder
    private RewardClaim(User user, CourseEnrollment courseEnrollment, Reward reward, LocalDateTime validUntil) {
        this.user = user;
        this.courseEnrollment = courseEnrollment;
        this.reward = reward;
        this.validUntil = validUntil;
        this.status = "claimed";
    }

    public void changeStatus(String status) {
        this.status = status;
    }
}
