package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
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
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id")
    private Reward reward;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private CourseType type;

    @Column(nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "is_ordered", nullable = false)
    private Boolean isOrdered;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Builder
    private Course(Organization organization, User creator, Reward reward, String name, String description,
                    CourseType type, CourseStatus status, Boolean isOrdered) {
        this.organization = organization;
        this.creator = creator;
        this.reward = reward;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status == null ? CourseStatus.DRAFT : status;
        this.isOrdered = isOrdered == null || isOrdered;
        this.viewCount = 0L;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void changeIsOrdered(boolean isOrdered) {
        this.isOrdered = isOrdered;
    }

    public void changeStatus(CourseStatus status) {
        this.status = status;
    }

    public void changeReward(Reward reward) {
        this.reward = reward;
    }

    public void approve(Reward bonusReward) {
        this.status = CourseStatus.PUBLISHED;
        if (bonusReward != null) {
            this.reward = bonusReward;
        }
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
