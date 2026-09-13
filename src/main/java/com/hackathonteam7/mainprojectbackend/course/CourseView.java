package com.hackathonteam7.mainprojectbackend.course;

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

/** (course_id, user_id) 당 최초 조회 여부를 판별해 view_count 중복 집계를 막기 위한 기록. */
@Getter
@Entity
@Table(
        name = "course_views",
        uniqueConstraints = @UniqueConstraint(name = "uq_course_view", columnNames = {"course_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseView extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private CourseView(Course course, User user) {
        this.course = course;
        this.user = user;
    }
}
