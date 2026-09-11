package com.hackathonteam7.mainprojectbackend.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrganizationIdOrderById(Long organizationId);

    Optional<Course> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Course> findAllByOrganizationIdAndStatusAndTypeIn(Long organizationId, CourseStatus status, List<CourseType> types);

    List<Course> findAllByRewardId(Long rewardId);

    long countByRewardId(Long rewardId);
}
