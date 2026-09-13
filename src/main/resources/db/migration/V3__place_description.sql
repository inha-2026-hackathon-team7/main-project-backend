-- =========================================================
-- V3__place_description.sql  (MySQL 8.0.16+)
-- 유저용 코스 상세(GET /courses/{id})의 places[] 가 장소별 설명을 보여줘야 하는데,
-- V1 의 places 에는 description 컬럼이 없어 additive 하게 추가한다.
-- =========================================================
ALTER TABLE places
  ADD COLUMN description TEXT NULL AFTER category;
