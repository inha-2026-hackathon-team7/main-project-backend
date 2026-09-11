package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseCreatedAtEntity;
import com.hackathonteam7.mainprojectbackend.place.Place;
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

@Getter
@Entity
@Table(
        name = "course_places",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_course_place", columnNames = {"course_id", "place_id"}),
                @UniqueConstraint(name = "uq_course_visit_order", columnNames = {"course_id", "visit_order"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePlace extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Builder
    private CoursePlace(Course course, Place place, Integer visitOrder) {
        this.course = course;
        this.place = place;
        this.visitOrder = visitOrder;
    }
}
