-- =========================================================
-- 지방 소멸 대응 관광 앱 - DB 스키마 (MySQL 8.0.16+ 기준)
-- CHECK 제약조건은 MySQL 8.0.16 이상에서만 실제로 강제됩니다.
-- =========================================================

CREATE TABLE organizations (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  logo         VARCHAR(255) NULL,
  type         ENUM('government','company','facility') NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(50) NOT NULL,
  email        VARCHAR(255) NOT NULL,
  password     VARCHAR(255) NOT NULL COMMENT '해시 저장 (bcrypt/argon2 등)',
  role         VARCHAR(30) NOT NULL DEFAULT 'user',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organization_members (
  id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  organization_id  BIGINT UNSIGNED NOT NULL,
  user_id          BIGINT UNSIGNED NOT NULL,
  role             VARCHAR(30) NOT NULL,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 한 유저는 한 조직에 한 멤버십(역할)만 가진다고 가정 (필요 없으면 제거)
  UNIQUE KEY uq_org_member (organization_id, user_id),
  CONSTRAINT fk_org_members_org  FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_org_members_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE regions (
  id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  organization_id  BIGINT UNSIGNED NOT NULL,
  type             VARCHAR(50) NULL,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_regions_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE places (
  id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  region_id        BIGINT UNSIGNED NOT NULL,
  organization_id  BIGINT UNSIGNED NOT NULL,
  latitude         DECIMAL(10,7) NOT NULL,
  longitude        DECIMAL(10,7) NOT NULL,
  category         VARCHAR(50) NULL,
  image_url        VARCHAR(255) NULL,
  qrcode_string    VARCHAR(255) NOT NULL,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_places_qrcode (qrcode_string),
  CONSTRAINT fk_places_region FOREIGN KEY (region_id) REFERENCES regions(id),
  CONSTRAINT fk_places_org    FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE courses (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  organization_id   BIGINT UNSIGNED NOT NULL,
  creator_user_id   BIGINT UNSIGNED NULL,
  name              VARCHAR(100) NOT NULL,
  description       TEXT NULL,
  type              ENUM('official','user','ai') NOT NULL,
  status            VARCHAR(20) NOT NULL DEFAULT 'draft',
  is_ordered        BOOLEAN NOT NULL DEFAULT TRUE,
  view_count        INT UNSIGNED NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_courses_org     FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_courses_creator FOREIGN KEY (creator_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE course_places (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  course_id    BIGINT UNSIGNED NOT NULL,
  place_id     BIGINT UNSIGNED NOT NULL,
  visit_order  INT UNSIGNED NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_course_place (course_id, place_id),
  CONSTRAINT fk_course_places_course FOREIGN KEY (course_id) REFERENCES courses(id),
  CONSTRAINT fk_course_places_place  FOREIGN KEY (place_id) REFERENCES places(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE course_enrollments (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  course_id     BIGINT UNSIGNED NOT NULL,
  user_id       BIGINT UNSIGNED NOT NULL,
  status        ENUM('active','complete','abandoned') NOT NULL DEFAULT 'active',
  started_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at  DATETIME NULL,
  UNIQUE KEY uq_course_enrollment (course_id, user_id),
  CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses(id),
  CONSTRAINT fk_enrollments_user   FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 반영 ①: UNIQUE(enrollment, course_place) + stamped_at 추가
CREATE TABLE course_stamps (
  id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  course_enrollment_id   BIGINT UNSIGNED NOT NULL,
  course_place_id        BIGINT UNSIGNED NOT NULL,
  stamped_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_course_stamp (course_enrollment_id, course_place_id),
  CONSTRAINT fk_stamps_enrollment  FOREIGN KEY (course_enrollment_id) REFERENCES course_enrollments(id),
  CONSTRAINT fk_stamps_course_place FOREIGN KEY (course_place_id) REFERENCES course_places(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rewards (
  id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  organization_id  BIGINT UNSIGNED NOT NULL,
  name             VARCHAR(100) NOT NULL,
  description      TEXT NULL,
  image_url        VARCHAR(255) NULL,
  stock            INT NOT NULL DEFAULT 0,
  valid_until      DATETIME NULL,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_rewards_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
  -- 반영 ②: 원자적 차감 로직(애플리케이션)을 보완하는 DB 레벨 안전장치
  CONSTRAINT chk_rewards_stock CHECK (stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reward_claims (
  id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id                BIGINT UNSIGNED NOT NULL,
  course_enrollment_id   BIGINT UNSIGNED NOT NULL,
  reward_id              BIGINT UNSIGNED NOT NULL,
  valid_until            DATETIME NULL,
  status                 VARCHAR(20) NOT NULL DEFAULT 'claimed',
  claimed_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_claims_user       FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_claims_enrollment FOREIGN KEY (course_enrollment_id) REFERENCES course_enrollments(id),
  CONSTRAINT fk_claims_reward     FOREIGN KEY (reward_id) REFERENCES rewards(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT UNSIGNED NOT NULL,
  place_id     BIGINT UNSIGNED NOT NULL,
  content      TEXT NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_review_user_place (user_id, place_id),
  CONSTRAINT fk_reviews_user  FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_reviews_place FOREIGN KEY (place_id) REFERENCES places(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_likes (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT UNSIGNED NOT NULL,
  review_id    BIGINT UNSIGNED NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_review_like (user_id, review_id),
  CONSTRAINT fk_review_likes_user   FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_review_likes_review FOREIGN KEY (review_id) REFERENCES reviews(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 반영 ③: place_id 또는 (latitude, longitude) 중 정확히 하나만 채워지도록 배타 처리
CREATE TABLE posts (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  region_id    BIGINT UNSIGNED NOT NULL,
  user_id      BIGINT UNSIGNED NOT NULL,
  content      TEXT NOT NULL,
  place_id     BIGINT UNSIGNED NULL,
  latitude     DECIMAL(10,7) NULL,
  longitude    DECIMAL(10,7) NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_posts_region FOREIGN KEY (region_id) REFERENCES regions(id),
  CONSTRAINT fk_posts_user   FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_posts_place  FOREIGN KEY (place_id) REFERENCES places(id),
  CONSTRAINT chk_posts_location CHECK (
    (place_id IS NOT NULL AND latitude IS NULL AND longitude IS NULL)
    OR
    (place_id IS NULL AND latitude IS NOT NULL AND longitude IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_likes (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT UNSIGNED NOT NULL,
  post_id    BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_post_like (user_id, post_id),
  CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
