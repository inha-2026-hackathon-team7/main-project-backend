package com.hackathonteam7.mainprojectbackend.course;

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
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "course_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uq_course_enrollment", columnNames = {"course_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private CourseEnrollmentStatus status;

    @CreatedDate
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private CourseEnrollment(Course course, User user) {
        this.course = course;
        this.user = user;
        this.status = CourseEnrollmentStatus.ACTIVE;
    }

    public void complete() {
        this.status = CourseEnrollmentStatus.COMPLETE;
        this.completedAt = LocalDateTime.now();
    }

    public void abandon() {
        this.status = CourseEnrollmentStatus.ABANDONED;
    }
}
