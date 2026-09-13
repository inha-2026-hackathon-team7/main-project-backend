-- =========================================================
-- V4__course_views.sql  (MySQL 8.0.16+)
-- courses.view_count 가 새로고침마다 무한히 오르는 문제를 막기 위해, 사용자별 조회 기록을
-- 별도로 남기고 (course_id, user_id) 조합당 최초 조회에서만 view_count 를 올린다.
-- =========================================================
CREATE TABLE course_views (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  course_id   BIGINT UNSIGNED NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_course_view (course_id, user_id),
  CONSTRAINT fk_course_views_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
  CONSTRAINT fk_course_views_user   FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
