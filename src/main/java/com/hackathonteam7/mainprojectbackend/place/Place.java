package com.hackathonteam7.mainprojectbackend.place;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.region.Region;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "places",
        uniqueConstraints = @UniqueConstraint(name = "uq_places_qrcode", columnNames = "qrcode_string")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    /** regions.organization_id 와 항상 일치해야 한다 (DB 의 fk_places_region_org 복합 FK 가 강제). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 50)
    private String category;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /** 서버가 UUID.randomUUID() 로 생성한다. 요청 본문으로 받은 값은 사용하지 않는다. */
    @Column(name = "qrcode_string", nullable = false, length = 255)
    private String qrcodeString;

    @Builder
    private Place(Region region, Organization organization, String name, BigDecimal latitude,
                   BigDecimal longitude, String category, String imageUrl, String qrcodeString) {
        this.region = region;
        this.organization = organization;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.imageUrl = imageUrl;
        this.qrcodeString = qrcodeString;
    }

    public void update(String name, BigDecimal latitude, BigDecimal longitude, String category, String imageUrl) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.imageUrl = imageUrl;
    }
}
