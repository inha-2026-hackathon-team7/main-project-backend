package com.hackathonteam7.mainprojectbackend.course;

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
        name = "course_stamps",
        uniqueConstraints = @UniqueConstraint(name = "uq_course_stamp", columnNames = {"course_enrollment_id", "course_place_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_enrollment_id", nullable = false)
    private CourseEnrollment courseEnrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_place_id", nullable = false)
    private CoursePlace coursePlace;

    @CreatedDate
    @Column(name = "stamped_at", nullable = false, updatable = false)
    private LocalDateTime stampedAt;

    @Builder
    private CourseStamp(CourseEnrollment courseEnrollment, CoursePlace coursePlace) {
        this.courseEnrollment = courseEnrollment;
        this.coursePlace = coursePlace;
    }
}
