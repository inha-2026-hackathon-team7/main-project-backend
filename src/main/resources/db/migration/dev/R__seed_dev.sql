-- =========================================================
-- R__seed_dev.sql  (Flyway repeatable migration, dev 프로파일 전용)
-- 프론트 어드민 프로토타입 화면과 대조할 수 있는 목업 데이터 (CLAUDE.md §8).
-- application-dev.properties 가 spring.flyway.locations 에 이 디렉터리를 추가할 때만
-- 스캔된다 — 기본/운영 설정(classpath:db/migration)에는 잡히지 않는다.
--
-- 비어 있는 로컬 DB 에 한 번 적용하는 용도다. 이미 데이터가 있는 상태에서 다시 적용하면
-- regions.uq_regions_org_name / users.uq_users_email 등에 걸려 실패한다 — 재적용하려면
-- 컨테이너 볼륨을 초기화한다 (docker compose down -v).
--
-- CLAUDE.md §8 은 "코스 6(official 4 / user 1 / ai 1)"과 "검수 대기 4(ai 2 / user 2)"를
-- 동시에 명시하는데, user+ai 가 1+1 뿐이면 검수 대기가 2 밖에 안 나와 서로 모순된다.
-- GET /admin/courses/pending 화면 검증이 목적이므로 검수 대기 4를 기준으로 삼아
-- user 2 / ai 2 로 채워 코스 총 8개로 구성했다.
-- =========================================================

-- ── 조직 1 + owner ──────────────────────────────────────────
INSERT INTO organizations (name, logo, type)
VALUES ('성수문화재단', NULL, 'facility');

SET @org_id = LAST_INSERT_ID();

-- password = "password1234" 의 BCrypt 해시
INSERT INTO users (name, email, password, role)
VALUES ('김성수', 'admin@seongsu.or.kr', '$2a$10$0XiTLAL6jCQLt5lq8I0CkeGD/LTMl/8Y4YocV2VsvjzFUR3I8zew2', 'organization');

SET @admin_user_id = LAST_INSERT_ID();

INSERT INTO organization_members (organization_id, user_id, role)
VALUES (@org_id, @admin_user_id, 'owner');

-- user/ai 코스의 작성자로 쓸 일반 사용자 1명
INSERT INTO users (name, email, password, role)
VALUES ('이지역', 'resident@example.com', '$2a$10$0XiTLAL6jCQLt5lq8I0CkeGD/LTMl/8Y4YocV2VsvjzFUR3I8zew2', 'user');

SET @resident_user_id = LAST_INSERT_ID();

-- ── 지역 5 ──────────────────────────────────────────────────
INSERT INTO regions (name, organization_id, type) VALUES
    ('성수·서울숲', @org_id, '도심'),
    ('북촌·삼청', @org_id, '문화'),
    ('해운대·마린시티', @org_id, '해안'),
    ('제주 동부', @org_id, '자연'),
    ('전주 한옥마을', @org_id, '문화');

SET @region_seongsu = (SELECT id FROM regions WHERE organization_id = @org_id AND name = '성수·서울숲');
SET @region_bukchon = (SELECT id FROM regions WHERE organization_id = @org_id AND name = '북촌·삼청');
SET @region_haeundae = (SELECT id FROM regions WHERE organization_id = @org_id AND name = '해운대·마린시티');
SET @region_jeju = (SELECT id FROM regions WHERE organization_id = @org_id AND name = '제주 동부');
SET @region_jeonju = (SELECT id FROM regions WHERE organization_id = @org_id AND name = '전주 한옥마을');

-- ── Place 11 (지역별 2~3개, 실좌표) ──────────────────────────
INSERT INTO places (name, region_id, organization_id, latitude, longitude, category, qrcode_string) VALUES
    ('성수 카페거리', @region_seongsu, @org_id, 37.5445000, 127.0557000, '카페', UUID()),
    ('서울숲', @region_seongsu, @org_id, 37.5443000, 127.0374000, '공원', UUID()),
    ('언더스탠드에비뉴', @region_seongsu, @org_id, 37.5460000, 127.0410000, '복합문화공간', UUID()),
    ('북촌한옥마을', @region_bukchon, @org_id, 37.5826000, 126.9830000, '관광지', UUID()),
    ('삼청동길', @region_bukchon, @org_id, 37.5824000, 126.9809000, '거리', UUID()),
    ('해운대해수욕장', @region_haeundae, @org_id, 35.1587000, 129.1604000, '해변', UUID()),
    ('마린시티', @region_haeundae, @org_id, 35.1533000, 129.1466000, '야경명소', UUID()),
    ('성산일출봉', @region_jeju, @org_id, 33.4581000, 126.9425000, '자연', UUID()),
    ('섭지코지', @region_jeju, @org_id, 33.4237000, 126.9280000, '자연', UUID()),
    ('전주한옥마을', @region_jeonju, @org_id, 35.8155000, 127.1522000, '관광지', UUID()),
    ('경기전', @region_jeonju, @org_id, 35.8143000, 127.1481000, '문화유산', UUID());

SET @place_seongsu_cafe = (SELECT id FROM places WHERE organization_id = @org_id AND name = '성수 카페거리');
SET @place_seoulforest = (SELECT id FROM places WHERE organization_id = @org_id AND name = '서울숲');
SET @place_understand = (SELECT id FROM places WHERE organization_id = @org_id AND name = '언더스탠드에비뉴');
SET @place_bukchon = (SELECT id FROM places WHERE organization_id = @org_id AND name = '북촌한옥마을');
SET @place_samcheong = (SELECT id FROM places WHERE organization_id = @org_id AND name = '삼청동길');
SET @place_haeundae = (SELECT id FROM places WHERE organization_id = @org_id AND name = '해운대해수욕장');
SET @place_marincity = (SELECT id FROM places WHERE organization_id = @org_id AND name = '마린시티');
SET @place_seongsan = (SELECT id FROM places WHERE organization_id = @org_id AND name = '성산일출봉');
SET @place_seopjikoji = (SELECT id FROM places WHERE organization_id = @org_id AND name = '섭지코지');
SET @place_jeonju = (SELECT id FROM places WHERE organization_id = @org_id AND name = '전주한옥마을');
SET @place_gyeonggijeon = (SELECT id FROM places WHERE organization_id = @org_id AND name = '경기전');

-- ── 리워드 4 (포인트 2 / 쿠폰 2, 하나는 품절, 하나는 재고 임박) ──
INSERT INTO rewards (organization_id, name, kind, description, stock, valid_until) VALUES
    (@org_id, '성수 스탬프 포인트', 'point', '코스 완주 시 적립되는 기본 포인트', 1000, NULL),
    (@org_id, '코스 완주 포인트', 'point', '완주 인증 포인트 (재고 소진 임박)', 40, NULL),
    (@org_id, '로컬 카페 할인 쿠폰', 'coupon', '성수 카페거리 제휴 할인 쿠폰 (품절)', 0, '2026-12-31 23:59:59'),
    (@org_id, '전주 한옥 체험 쿠폰', 'coupon', '한옥 게스트하우스 체험 쿠폰', 200, '2026-12-31 23:59:59');

SET @reward_stamp_point = (SELECT id FROM rewards WHERE organization_id = @org_id AND name = '성수 스탬프 포인트');

-- ── 코스 8 (official 4: published 2 / draft 1 / archived 1, user 2, ai 2 — 모두 검수 대기) ──
INSERT INTO courses (organization_id, creator_user_id, reward_id, name, description, type, status, is_ordered) VALUES
    (@org_id, @admin_user_id, @reward_stamp_point, '성수 로컬 크래프트 투어', '성수동 카페거리와 서울숲을 잇는 공식 코스', 'official', 'published', TRUE),
    (@org_id, @admin_user_id, NULL, '북촌 골목길 산책', '북촌한옥마을과 삼청동을 잇는 공식 코스', 'official', 'published', TRUE),
    (@org_id, @admin_user_id, NULL, '해운대 마린시티 야경 코스', '해운대에서 마린시티 야경까지', 'official', 'draft', TRUE),
    (@org_id, @admin_user_id, NULL, '제주 성산 일출 코스', '작년 시즌 운영 후 보관된 코스', 'official', 'archived', TRUE),
    (@org_id, @resident_user_id, NULL, '전주 한옥마을 야시장 코스', '주민이 제안한 야시장 코스', 'user', 'draft', TRUE),
    (@org_id, NULL, NULL, '서울숲 피크닉 코스 (AI 추천)', 'AI 가 생성한 피크닉 코스 초안', 'ai', 'draft', FALSE),
    (@org_id, @resident_user_id, NULL, '삼청동 북카페 투어', '주민이 제안한 북카페 투어', 'user', 'draft', TRUE),
    (@org_id, NULL, NULL, '마린시티 야경 드라이브 (AI 추천)', 'AI 가 생성한 드라이브 코스 초안', 'ai', 'draft', FALSE);

SET @course_seongsu = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '성수 로컬 크래프트 투어');
SET @course_bukchon = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '북촌 골목길 산책');
SET @course_haeundae = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '해운대 마린시티 야경 코스');
SET @course_jeju = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '제주 성산 일출 코스');
SET @course_jeonju = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '전주 한옥마을 야시장 코스');
SET @course_seoulforest_ai = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '서울숲 피크닉 코스 (AI 추천)');
SET @course_samcheong_user = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '삼청동 북카페 투어');
SET @course_marincity_ai = (SELECT id FROM courses WHERE organization_id = @org_id AND name = '마린시티 야경 드라이브 (AI 추천)');

-- ── 코스 구성 (코스별 2~3 Place) ─────────────────────────────
INSERT INTO course_places (course_id, place_id, visit_order) VALUES
    (@course_seongsu, @place_seongsu_cafe, 1),
    (@course_seongsu, @place_seoulforest, 2),
    (@course_seongsu, @place_understand, 3),
    (@course_bukchon, @place_bukchon, 1),
    (@course_bukchon, @place_samcheong, 2),
    (@course_haeundae, @place_haeundae, 1),
    (@course_haeundae, @place_marincity, 2),
    (@course_jeju, @place_seongsan, 1),
    (@course_jeju, @place_seopjikoji, 2),
    (@course_jeonju, @place_jeonju, 1),
    (@course_jeonju, @place_gyeonggijeon, 2),
    (@course_seoulforest_ai, @place_seoulforest, 1),
    (@course_seoulforest_ai, @place_understand, 2),
    (@course_samcheong_user, @place_samcheong, 1),
    (@course_samcheong_user, @place_bukchon, 2),
    (@course_marincity_ai, @place_marincity, 1),
    (@course_marincity_ai, @place_haeundae, 2);

-- ── 통계 검증용 대량 enrollment: 코스 1(성수 로컬 크래프트 투어)에 864건 ──
-- completed 512 / abandoned 352, 그중 480건은 reward_claimed.
INSERT INTO users (name, email, password, role)
SELECT
    CONCAT('시드유저', LPAD(n, 4, '0')),
    CONCAT('seed-user-', LPAD(n, 4, '0'), '@example.com'),
    '$2a$10$0XiTLAL6jCQLt5lq8I0CkeGD/LTMl/8Y4YocV2VsvjzFUR3I8zew2',
    'user'
FROM (
    WITH RECURSIVE seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1 FROM seq WHERE n < 864
    )
    SELECT n FROM seq
) AS numbers;

INSERT INTO course_enrollments (course_id, user_id, status, completed_at)
SELECT
    @course_seongsu,
    u.id,
    CASE WHEN numbers.n <= 512 THEN 'complete' ELSE 'abandoned' END,
    CASE WHEN numbers.n <= 512 THEN NOW() ELSE NULL END
FROM (
    WITH RECURSIVE seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1 FROM seq WHERE n < 864
    )
    SELECT n FROM seq
) AS numbers
JOIN users u ON u.email = CONCAT('seed-user-', LPAD(numbers.n, 4, '0'), '@example.com');

INSERT INTO reward_claims (user_id, course_enrollment_id, reward_id, status)
SELECT ce.user_id, ce.id, @reward_stamp_point, 'claimed'
FROM course_enrollments ce
WHERE ce.course_id = @course_seongsu
  AND ce.status = 'complete'
ORDER BY ce.id
LIMIT 480;
