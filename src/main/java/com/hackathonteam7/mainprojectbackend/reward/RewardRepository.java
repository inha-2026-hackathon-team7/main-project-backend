package com.hackathonteam7.mainprojectbackend.reward;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findAllByOrganizationIdOrderById(Long organizationId);

    Optional<Reward> findByIdAndOrganizationId(Long id, Long organizationId);
}
