# 지역 기반 코스/장소/리워드 어드민 — 백엔드 작업 지침

이 문서는 **백엔드 저장소 루트의 `CLAUDE.md`** 로 쓰기 위해 작성됐다.
프론트엔드 어드민 프로토타입(6개 화면)이 이미 존재하고, 그 화면들이 기대하는 계약이
아래에 확정돼 있다. **스펙을 바꾸지 말고 그대로 구현할 것.** 변경이 필요하면 먼저 질문한다.

---

## 1. 스택 / 버

| 항목 | 값 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.3.x |
| 빌드 | Gradle (Kotlin DSL) |
| 영속성 | Spring Data JPA + Hibernate 6 |
| DB | MySQL 8.4 LTS (로컬은 docker-compose, 테스트는 Testcontainers) |
| 마이그레이션 | Flyway (`src/main/resources/db/migration/V1__init.sql`) |
| 인증 | Spring Security 6 + `io.jsonwebtoken:jjwt-api/impl/jackson` 0.12.x |
| QR | `com.google.zxing:core` + `javase` |
| 매핑 | 수동 매핑 (정적 팩토리 `XxxResponse.from(entity)`). MapStruct/ModelMapper 금지 |
| 검증 | `spring-boot-starter-validation` (Jakarta Bean Validation) |

`ddl-auto` 는 **`validate`** 로 고정한다. 스키마는 Flyway 가 단독 소유한다.

---

## 2. 패키지 구조

```
com.hackathonteam7.mainprojectbackend
├── config/           SecurityConfig, JwtProperties, WebConfig, OpenApiConfig
├── security/         JwtTokenProvider, JwtAuthenticationFilter, PrincipalUser
├── common/
│   ├── error/        ApiException, ErrorCode, GlobalExceptionHandler, ErrorResponse
│   └── audit/        BaseTimeEntity (createdAt, updatedAt)
├── auth/             AuthController, AuthService, dto/
├── organization/     Organization, OrganizationMember, repository
├── user/             User, Role, repository
├── region/           Region, RegionController, RegionService, RegionRepository, dto/
├── place/            Place, PlaceController, PlaceService, PlaceRepository, QrCodeService, dto/
├── course/           Course, CoursePlace, CourseEnrollment, controller/service/repository, dto/
├── reward/           Reward, RewardController, RewardService, RewardRepository, dto/
└── review/           ReviewController, ReviewService (검수 큐: pending/approve/reject)
```

도메인별 수직 슬라이스. `service` 는 트랜잭션 경계이자 권한 검사 지점이고,
`controller` 는 DTO 변환만 한다. Repository 를 controller 에서 직접 쓰지 않는다.

---

## 3. 데이터 모델

모든 엔티티는 `BaseTimeEntity`(`@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`)를 상속한다.
`@Setter` 를 클래스 레벨에 붙이지 않는다 — 상태 변경은 의도를 가진 메서드(`rename`, `changeStatus`, `restock`)로만 한다.

### organizations
`id`(PK), `name`(not null), `type`, `created_at`, `updated_at`

### users
`id`(PK), `name`, `email`(**unique, not null**), `password`(BCrypt 해시), `role`(enum: `USER`, `ORGANIZATION`)

### organization_members
`id`(PK), `organization_id`(FK→organizations), `user_id`(FK→users), `role`(enum: `OWNER`, `MANAGER`)
`unique (organization_id, user_id)`

### regions
`id`(PK), `organization_id`(FK→organizations, **not null**), `name`(not null), `type`
`unique (organization_id, name)`

### places
`id`(PK), `region_id`(FK→regions, **not null**), `name`(not null),
`latitude`(`numeric(10,7)`, not null), `longitude`(`numeric(10,7)`, not null),
`category`, `image_url`, `qrcode_string`(**unique, not null**)

- `qrcode_string` 은 **서버가** `UUID.randomUUID().toString()` 으로 생성한다. 요청 본문에서 받으면 무시한다(400 아님, 그냥 버린다).
- `region_id` FK 는 `ON DELETE RESTRICT`. 이게 "하위 place 가 있으면 지역 삭제 실패"의 근거다.
- 좌표는 `BigDecimal` 로 다룬다. `double` 금지.

### courses
`id`(PK), `organization_id`(FK), `name`(not null), `description`(text),
`type`(enum: `OFFICIAL`, `USER`, `AI`, not null), `status`(enum: `DRAFT`, `PUBLISHED`, `ARCHIVED`, not null, default `DRAFT`),
`is_ordered`(boolean, not null, default true), `reward_id`(FK→rewards, **nullable**, `ON DELETE RESTRICT`),
`view_count`(bigint, not null, default 0), `creator_user_id`(FK→users, nullable)

### course_places
`id`(PK), `course_id`(FK→courses, `ON DELETE CASCADE`), `place_id`(FK→places, **`ON DELETE RESTRICT`**), `visit_order`(int, not null)
`unique (course_id, place_id)`, `unique (course_id, visit_order)`

- place FK 가 RESTRICT 인 것이 "코스에서 참조 중인 Place 삭제 불가"의 근거다.
- `unique (course_id, visit_order)` 때문에 순서 재배치는 **한 트랜잭션 안에서 전량 삭제 후 재삽입**하고 `flush()` 를 사이에 넣어야 한다 (아래 5.4 참고).

### course_enrollments
`id`(PK), `course_id`(FK→courses, RESTRICT), `user_id`(FK→users),
`status`(enum: `IN_PROGRESS`, `COMPLETED`, `ABANDONED`), `reward_claimed`(boolean, not null, default false),
`started_at`, `completed_at`
`unique (course_id, user_id)`

- 이 테이블에 행이 있으면 코스 하드 삭제를 막는다 (409 → 프론트는 `status=ARCHIVED` 전환을 유도).

### rewards
`id`(PK), `organization_id`(FK), `name`(not null), `kind`(enum: `POINT`, `COUPON`),
`description`(text), `image_url`, `stock`(int, not null, `check (stock >= 0)`), `valid_until`(date, nullable)

### course_reviews (검수 이력)
`id`(PK), `course_id`(FK), `reviewer_user_id`(FK→users), `decision`(enum: `APPROVED`, `REJECTED`),
`reason`(text, nullable), `bonus_reward_id`(FK→rewards, nullable), `created_at`

---

## 4. 인증 / 인가

### 토큰
- `POST /admin/auth/register`, `POST /auth/login` 만 `permitAll`. 나머지 `/admin/**` 는 `hasRole("ORGANIZATION")`.
- JWT HS256. 클레임: `sub`(userId), `email`, `role`, `orgId`. 만료 1시간. 시크릿은 `JWT_SECRET` 환경변수(최소 32바이트), `JwtProperties` 로 바인딩.
- `JwtAuthenticationFilter` 는 `UsernamePasswordAuthenticationFilter` 앞에 둔다. `Authorization: Bearer <token>` 만 읽는다.
- `SecurityContext` 에는 `PrincipalUser(userId, orgId, role)` 를 넣는다. 컨트롤러는 `@AuthenticationPrincipal PrincipalUser me` 로 받는다.
- 세션 `STATELESS`, CSRF disabled, CORS 는 프론트 오리진만 허용.

### 조직 스코프 — 가장 중요한 규칙
`/admin/**` 의 **모든** 조회·수정·삭제는 `me.orgId()` 로 스코프된다.
다른 조직의 리소스는 403 이 아니라 **404** 로 응답한다(존재 여부 노출 방지).

```java
// 모든 서비스에서 이 패턴을 쓴다
Region region = regionRepository.findByIdAndOrganizationId(id, me.orgId())
        .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
```

`findById(id)` 만 쓰고 소유권을 검사하지 않는 코드는 리뷰에서 반드시 거른다.

### 가입 트랜잭션
`POST /admin/auth/register` 는 하나의 `@Transactional` 안에서
`organizations` → `users(role=ORGANIZATION)` → `organization_members(role=OWNER)` 를 생성하고,
곧바로 발급한 `access_token` 을 함께 반환한다(별도 로그인 호출 없음).
이메일 중복은 409 `EMAIL_DUPLICATED`.

---

## 5. 엔드포인트

모든 경로는 프론트가 이미 호출하고 있는 형태다. 경로·필드명을 바꾸지 않는다.
요청/응답 바디의 키는 **snake_case** (`spring.jackson.property-naming-strategy=SNAKE_CASE`).

### 5.1 인증
| Method | Path | Request | Response |
| --- | --- | --- | --- |
| POST | `/admin/auth/register` | `organization_name, organization_type, admin_name, admin_email, admin_password` | 201 `{organization_id, user_id, access_token}` |
| POST | `/auth/login` | `email, password` | 200 `{access_token, user:{id, name, role}}` |

`role` 은 소문자 문자열(`"organization"`)로 내려준다 — 프론트가 이 값으로 라우팅한다.

### 5.2 지역
| Method | Path | 비고 |
| --- | --- | --- |
| GET | `/admin/regions` | `[{id, name, type, place_count, course_count}]` — 프론트 목록이 두 카운트를 표시한다. **N+1 금지**: 카운트는 단일 JPQL projection 으로 계산 |
| POST | `/admin/regions` | `{name, type?}` → 201 |
| PUT | `/admin/regions/{id}` | `{name?, type?}` |
| DELETE | `/admin/regions/{id}` | 204. 하위 place 존재 시 **409 `REGION_HAS_PLACES`** |

`place_count` / `course_count` 예시 쿼리:

```java
@Query("""
  select new com.example.courseadmin.region.dto.RegionSummary(
      r.id, r.name, r.type,
      (select count(p) from Place p where p.region = r),
      (select count(distinct cp.course) from CoursePlace cp where cp.place.region = r))
  from Region r where r.organization.id = :orgId order by r.id
""")
List<RegionSummary> findSummaries(Long orgId);
```

삭제는 DB FK 예외를 잡지 말고 **선제 검사**한다. `DataIntegrityViolationException` 을 409 로 번역하는 건
마지막 안전망일 뿐이고, 정상 경로는 카운트 확인 → `ApiException` 이다.

### 5.3 Place
| Method | Path | 비고 |
| --- | --- | --- |
| GET | `/admin/places?region_id=` | `region_id` 는 optional |
| POST | `/admin/places` | `{name, region_id, latitude, longitude, category?, image_url?}` → 201, 응답에 `qrcode_string` 포함 |
| GET | `/admin/places/{id}` | 전체 필드 + `referencing_courses:[{id, name, status}]` (프론트 삭제 차단 모달이 이 목록을 보여준다) |
| PUT | `/admin/places/{id}` | `region_id` 변경은 허용하지 않는다(요청에 있어도 무시) |
| DELETE | `/admin/places/{id}` | 204. `course_places` 참조 시 **409 `PLACE_IN_USE`** — 에러 `details` 에 참조 코스 목록을 담는다 |
| GET | `/admin/places/{id}/qrcode` | `Content-Type: image/png`, ZXing 으로 `qrcode_string` 인코딩. 512×512, margin 1, `Cache-Control: public, max-age=86400` |

좌표 검증: `@DecimalMin("-90") @DecimalMax("90")` (lat), `@DecimalMin("-180") @DecimalMax("180")` (lng).

### 5.4 코스
| Method | Path | 비고 |
| --- | --- | --- |
| GET | `/admin/courses?type=&status=` | `[{id, name, type, status, is_ordered, view_count, place_count, reward:{id,name}|null, participants}]` |
| POST | `/admin/courses` | `{name, description?, is_ordered, reward_id?}` → `type=OFFICIAL`, `status=DRAFT` 고정 |
| GET | `/admin/courses/{id}` | `places:[{course_place_id, place_id, name, visit_order, latitude, longitude}]` — 이름/좌표를 함께 내려야 프론트 2단 편집기가 추가 조회를 안 한다 |
| PUT | `/admin/courses/{id}` | `{name?, description?, status?, is_ordered?, reward_id?}` (`reward_id: null` 은 연결 해제로 처리 — JSON 에 키가 **없는 것**과 구분해야 하므로 `JsonNullable` 또는 `Optional<Optional<Long>>` 패턴 사용) |
| DELETE | `/admin/courses/{id}` | enrollment 없으면 204. 있으면 **409 `COURSE_HAS_ENROLLMENTS`** |
| PUT | `/admin/courses/{id}/places` | `[{place_id, visit_order}]` **전량 교체** |

`PUT /places` 구현 규칙:
1. `place_id` 들이 모두 같은 조직 소유인지 검사 → 아니면 404
2. `visit_order` 가 1..N 연속인지 검사 → 아니면 400 `INVALID_VISIT_ORDER`
3. `place_id` 중복 → 400 `DUPLICATE_PLACE`
4. 기존 `course_places` 전량 `deleteAllByCourseId` → **`entityManager.flush()`** → 신규 삽입
   (flush 를 빼면 `unique (course_id, visit_order)` 로 제약 위반이 난다)
5. 하나의 `@Transactional`. 응답은 갱신된 place 목록

### 5.5 리워드
| Method | Path | 비고 |
| --- | --- | --- |
| GET | `/admin/rewards` | `[{id, name, kind, description, stock, valid_until, linked_course_count}]` |
| POST | `/admin/rewards` | `{name, kind, description?, image_url?, stock, valid_until?}` |
| PUT | `/admin/rewards/{id}` | `{name?, stock?, valid_until?}` — 재고 보충도 이 엔드포인트 |
| DELETE | `/admin/rewards/{id}` | 204. 코스가 연결돼 있으면 **409 `REWARD_IN_USE`** |

### 5.6 통계 / 검수
| Method | Path | 비고 |
| --- | --- | --- |
| GET | `/admin/courses/{id}/stats` | `{view_count, participants, completed, abandoned, reward_claimed}` — enrollment 집계는 **단일 group-by 쿼리 1회**로 계산 |
| GET | `/admin/courses/pending?type=user\|ai` | `[{id, name, creator, created_at, place_count, ai_confidence?}]` — `status=DRAFT` 이고 `type in (USER, AI)` 인 코스 |
| POST | `/admin/courses/{id}/approve` | `{bonus_reward_id?}` → `status=PUBLISHED`, `course_reviews` 에 `APPROVED` 기록. `bonus_reward_id` 가 오면 코스의 `reward_id` 로 연결 |
| POST | `/admin/courses/{id}/reject` | `{reason?}` → `course_reviews` 에 `REJECTED` 기록, 코스 `status` 는 `DRAFT` 유지 |

approve/reject 는 **이미 처리된 코스에 재호출되면 409 `ALREADY_REVIEWED`** 로 막는다(프론트 큐에서 낙관적 제거가 일어나므로 멱등 방어가 필요).

### 5.7 아직 만들지 않을 것 (프론트에 "API 필요" 로 표시된 항목)
프론트 대시보드가 아래 세 가지를 제안 상태로 표시하고 있다. **요청받기 전까지 구현하지 않는다.**
- `GET /admin/courses/{id}/checkins` — Place 별 체크인 타임스탬프
- `GET /admin/courses/{id}/stats?group_by=day` — 일자별 추이
- `GET /admin/courses/{id}/stats?dimension=source` — 유입 경로

---

## 6. 에러 응답 — 프론트 문구와 1:1 대응

프론트는 HTTP status 와 `code` 로 분기해서 **차단 모달**을 띄운다.
`details` 배열은 그 모달의 "참조 목록" 에 그대로 렌더링된다. 형식을 지킬 것.

```json
{
  "code": "PLACE_IN_USE",
  "message": "코스에서 참조 중인 Place 는 삭제할 수 없습니다.",
  "details": [
    { "id": 1, "name": "성수 로컬 크래프트 투어", "note": "status=published" }
  ]
}
```

| code | status | 발생 지점 |
| --- | --- | --- |
| `REGION_HAS_PLACES` | 409 | 지역 삭제 — `details` = 하위 place 목록 |
| `PLACE_IN_USE` | 409 | Place 삭제 — `details` = 참조 코스 목록 |
| `REWARD_IN_USE` | 409 | 리워드 삭제 — `details` = 연결 코스 목록 |
| `COURSE_HAS_ENROLLMENTS` | 409 | 코스 삭제 — `details` = 참가/완주/수령 건수 |
| `ALREADY_REVIEWED` | 409 | approve/reject 재호출 |
| `EMAIL_DUPLICATED` | 409 | 가입 |
| `INVALID_VISIT_ORDER` / `DUPLICATE_PLACE` | 400 | 코스 구성 교체 |
| `VALIDATION_FAILED` | 400 | Bean Validation — `details` = `[{field, message}]` |
| `*_NOT_FOUND` | 404 | 조직 스코프 밖 접근 포함 |
| `UNAUTHORIZED` / `FORBIDDEN` | 401 / 403 | 토큰 없음·만료 / 권한 부족 |

`@RestControllerAdvice` 하나(`GlobalExceptionHandler`)에서 `ApiException`,
`MethodArgumentNotValidException`, `DataIntegrityViolationException`, `AccessDeniedException`,
`Exception`(→500, 스택트레이스는 로그만) 을 처리한다. 컨트롤러에 try-catch 를 쓰지 않는다.

---

## 7. 코딩 규칙

- **Lombok**: `@Getter`, `@Builder`, `@NoArgsConstructor(access = PROTECTED)`, `@RequiredArgsConstructor` 만. `@Data`, `@Setter`, `@EqualsAndHashCode` 금지.
- **DTO**: 전부 `record`. 요청 DTO 에 검증 애노테이션을 붙인다. 엔티티를 컨트롤러 밖으로 내보내지 않는다.
- **연관관계**: 전부 `LAZY`. 목록 조회는 DTO projection 또는 `@EntityGraph` 로 N+1 을 차단하고, 페이징과 `join fetch` 를 같이 쓰지 않는다.
- **`@Transactional`**: 서비스 메서드 단위. 조회는 `readOnly = true`.
- **테스트**: 도메인 규칙은 단위 테스트, 에러 계약(§6)은 `@SpringBootTest` + `MockMvc` 로 **status 와 `code` 를 검증**한다. DB 는 Testcontainers PostgreSQL — H2 로 대체하지 않는다(FK RESTRICT 동작이 달라진다).
- 주석은 "왜" 만 적는다. 자명한 코드에 주석을 달지 않는다.

---

## 8. 로컬 실행 시드 데이터

`db/migration/R__seed_dev.sql` (dev 프로파일에서만 적용)로 프론트 프로토타입과 **같은 목업**을 넣는다.
프론트 화면을 그대로 대조할 수 있어야 한다.

- 조직 1: `성수문화재단` / owner `admin@seongsu.or.kr`
- 지역 5: 성수·서울숲(도심), 북촌·삼청(문화), 해운대·마린시티(해안), 제주 동부(자연), 전주 한옥마을(문화)
- Place 11 (위 지역에 2~3개씩, 실제 좌표)
- 코스 6: official 4(published 2 / draft 1 / archived 1), user 1, ai 1
- 리워드 4: 포인트 2, 쿠폰 2 — 그중 하나는 `stock=0`, 하나는 `stock=40`(재고 임박 배너 확인용)
- 검수 대기 4: ai 2, user 2 (`status=DRAFT`)
- enrollment: 코스 1에 864건(completed 512, abandoned 352, reward_claimed 480) — 통계 화면 검증용

---

## 9. 작업 순서 (권장)

1. Flyway `V1__init.sql` 및 추가 수정사항 반영 (완료) 
2. 마이그레이션 기반 엔티티 + 리포지토리 + Testcontainers 기동
2. 인증(register/login) + Security 필터 + `PrincipalUser` — 이게 되면 나머지 전부의 스코프가 잡힌다
3. `GlobalExceptionHandler` + `ErrorCode` (§6 전체를 먼저 만들어 둔다)
4. 지역 → Place(+QR) → 리워드 → 코스 → 코스 구성 교체 → 통계 → 검수
5. 각 단계마다 §6 에러 케이스 통합 테스트를 같이 쓴다

시작 전에 확인할 것: PostgreSQL 버전, 배포 환경(로컬/Docker/클라우드), 프론트 오리진(CORS), `JWT_SECRET` 주입 방식.
