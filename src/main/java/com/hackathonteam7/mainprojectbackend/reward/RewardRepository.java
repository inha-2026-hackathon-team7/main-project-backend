package com.hackathonteam7.mainprojectbackend.reward;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findAllByOrganizationIdOrderById(Long organizationId);

    Optional<Reward> findByIdAndOrganizationId(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reward r where r.id = :id")
    Optional<Reward> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update Reward r set r.stock = r.stock - 1 where r.id = :id and r.stock > 0")
    int decrementStockIfAvailable(@Param("id") Long id);
}
