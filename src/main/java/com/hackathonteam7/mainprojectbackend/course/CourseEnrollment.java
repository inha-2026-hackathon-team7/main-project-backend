package com.hackathonteam7.mainprojectbackend.course;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
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

    /**
     * "이번 시도"가 시작된 시각. (course_id, user_id) 가 unique 라 재시작은 새 row 가 아니라
     * 이 row 를 리셋하는 방식이라서, 최초 생성 시각을 고정하는 @CreatedDate 로 두지 않고 직접 관리한다.
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private CourseEnrollment(Course course, User user) {
        this.course = course;
        this.user = user;
        this.status = CourseEnrollmentStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = CourseEnrollmentStatus.COMPLETE;
        this.completedAt = LocalDateTime.now();
    }

    public void abandon() {
        this.status = CourseEnrollmentStatus.ABANDONED;
    }

    /** ABANDONED 였던 참가를 새 시도로 리셋한다. 스탬프 삭제는 호출자(서비스)의 책임이다. */
    public void restart() {
        this.status = CourseEnrollmentStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.completedAt = null;
    }
}
