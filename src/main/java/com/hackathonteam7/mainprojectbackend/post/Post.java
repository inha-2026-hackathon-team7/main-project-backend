package com.hackathonteam7.mainprojectbackend.post;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import com.hackathonteam7.mainprojectbackend.place.Place;
import com.hackathonteam7.mainprojectbackend.region.Region;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** place_id 또는 (latitude, longitude) 중 정확히 하나만 채워진다 (DB 의 chk_posts_location). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Builder
    private Post(Region region, User user, String content, Place place, BigDecimal latitude, BigDecimal longitude) {
        boolean hasPlace = place != null;
        boolean hasCoordinate = latitude != null && longitude != null;
        if (hasPlace == hasCoordinate) {
            throw new IllegalArgumentException("place 또는 (latitude, longitude) 중 정확히 하나만 지정해야 합니다.");
        }
        this.region = region;
        this.user = user;
        this.content = content;
        this.place = place;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
