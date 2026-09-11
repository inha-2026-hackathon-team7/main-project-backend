-- =========================================================
-- V2__admin_additions.sql  (MySQL 8.0.16+)
-- V1 은 정본이므로 손대지 않고, 어드민 화면이 동작하기 위해 필요한 것만 additive 하게 추가한다.
-- 각 항목은 프론트 어드민의 어떤 동작을 위한 것인지 주석으로 남긴다.
-- =========================================================

-- ─────────────────────────────────────────────────────────
-- 1. 지역: 조직 내 이름 중복 금지 + 조직 스코프를 DB 가 강제하도록 하는 보조 유니크 키
--    (places 의 복합 FK 가 이 키를 참조한다)
-- ─────────────────────────────────────────────────────────
ALTER TABLE regions
  ADD UNIQUE KEY uq_regions_org_name (organization_id, name),
  ADD UNIQUE KEY uq_regions_id_org  (id, organization_id);

-- ─────────────────────────────────────────────────────────
-- 2. Place: organization_id 가 region 의 소유 조직과 어긋나는 것을 DB 레벨에서 차단
--    (초안은 region_id 와 organization_id 를 따로 들고 있어 애플리케이션 버그로 drift 가능)
-- ─────────────────────────────────────────────────────────
ALTER TABLE places DROP FOREIGN KEY fk_places_region;
ALTER TABLE places
  ADD CONSTRAINT fk_places_region_org
    FOREIGN KEY (region_id, organization_id) REFERENCES regions (id, organization_id);

-- ─────────────────────────────────────────────────────────
-- 3. 코스 구성: visit_order 필수 + 코스 내 순서 중복 금지
--    어드민의 "구성 전체 저장"(PUT /admin/courses/{id}/places)이 1..N 연속값을 보내므로
--    NULL 을 허용할 필요가 없다. 기존 NULL 행은 삽입 순서대로 번호를 채운다.
-- ─────────────────────────────────────────────────────────
SET @row := 0, @prev := 0;
UPDATE course_places cp
  JOIN (
    SELECT id,
           @row := IF(@prev = course_id, @row + 1, 1) AS new_order,
           @prev := course_id
    FROM course_places
    ORDER BY course_id, COALESCE(visit_order, 999999), id
  ) t ON t.id = cp.id
SET cp.visit_order = t.new_order
WHERE cp.visit_order IS NULL;

ALTER TABLE course_places
  MODIFY visit_order INT UNSIGNED NOT NULL,
  ADD UNIQUE KEY uq_course_visit_order (course_id, visit_order);

-- 코스를 지우면 구성은 함께 사라져야 한다 (place 쪽 FK 는 RESTRICT 유지 — 삭제 차단의 근거)
ALTER TABLE course_places DROP FOREIGN KEY fk_course_places_course;
ALTER TABLE course_places
  ADD CONSTRAINT fk_course_places_course
    FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE;

-- 참가 기록을 지우면 스탬프도 함께 사라져야 한다
ALTER TABLE course_stamps DROP FOREIGN KEY fk_stamps_enrollment;
ALTER TABLE course_stamps
  ADD CONSTRAINT fk_stamps_enrollment
    FOREIGN KEY (course_enrollment_id) REFERENCES course_enrollments (id) ON DELETE CASCADE;

-- ─────────────────────────────────────────────────────────
-- 4. 리워드 유형: 어드민 리워드 카드가 포인트 / 쿠폰을 구분해 표시한다
--    V1 의 다른 enum 들과 같은 소문자 표기를 따른다.
-- ─────────────────────────────────────────────────────────
ALTER TABLE rewards
  ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'point' AFTER name,
  ADD CONSTRAINT chk_rewards_kind CHECK (kind IN ('point', 'coupon'));

-- ─────────────────────────────────────────────────────────
-- 5. 코스 검수 이력
--    V1 의 reviews 는 "사용자가 장소에 남기는 후기"이고, 이 테이블은
--    "관리자가 user/ai 코스를 승인·반려한 기록"이다. 이름 혼동 주의.
--    POST /admin/courses/{id}/approve|reject 의 멱등 방어(ALREADY_REVIEWED)가 이 테이블에 의존한다.
-- ─────────────────────────────────────────────────────────
CREATE TABLE course_reviews (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  course_id         BIGINT UNSIGNED NOT NULL,
  reviewer_user_id  BIGINT UNSIGNED NOT NULL,
  decision          VARCHAR(20) NOT NULL,
  reason            TEXT NULL,
  bonus_reward_id   BIGINT UNSIGNED NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_course_reviews_course (course_id, created_at),
  CONSTRAINT chk_course_reviews_decision CHECK (decision IN ('approved', 'rejected')),
  CONSTRAINT fk_course_reviews_course   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
  CONSTRAINT fk_course_reviews_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users (id),
  CONSTRAINT fk_course_reviews_bonus    FOREIGN KEY (bonus_reward_id) REFERENCES rewards (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────
-- 6. 어드민 목록/통계 쿼리용 인덱스
--    MySQL 은 FK 의 선두 컬럼에 인덱스를 자동 생성하므로, 여기서는 실제 필터 조합만 보완한다.
-- ─────────────────────────────────────────────────────────
-- GET /admin/courses?type=&status=
CREATE INDEX idx_courses_org_status_type ON courses (organization_id, status, type);
-- GET /admin/courses/{id}/stats — status 별 group by
CREATE INDEX idx_enrollments_course_status ON course_enrollments (course_id, status);
-- reward_claimed 집계
CREATE INDEX idx_claims_reward ON reward_claims (reward_id);
-- GET /admin/courses/{id}/checkins — 구간별 체크인 추이
CREATE INDEX idx_stamps_place_time ON course_stamps (course_place_id, stamped_at);
-- Place 삭제 차단 검사(참조 코스 조회)
CREATE INDEX idx_course_places_place ON course_places (place_id);
