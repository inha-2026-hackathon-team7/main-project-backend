package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Entity
@Table(name = "rewards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reward extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private RewardKind kind;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /** DB 의 chk_rewards_stock (stock >= 0) 이 최종 방어선이다. */
    @Column(nullable = false)
    private Integer stock;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Builder
    private Reward(Organization organization, String name, RewardKind kind, String description,
                    String imageUrl, Integer stock, LocalDateTime validUntil) {
        this.organization = organization;
        this.name = name;
        this.kind = kind;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.validUntil = validUntil;
    }

    public void rename(String name) {
        this.name = name;
    }

    /** PUT /admin/rewards/{id} 의 stock 은 델타가 아니라 새 재고 수량이다 (재고 보충도 이 엔드포인트). */
    public void changeStock(int stock) {
        this.stock = stock;
    }

    public void changeValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }
}
