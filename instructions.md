# 인수인계 문서 (2026-09-14 기준)

이 문서는 지금까지 이 저장소에서 진행된 작업을 다음에 이어받는 에이전트/사람이 빠르게
파악할 수 있도록 정리한 것이다. `CLAUDE.md` (프로젝트 루트)가 원래 스펙 문서이고, 이 문서는
**그 스펙과 실제 구현이 어디서, 왜 갈라졌는지 + 지금 상태 + 당장 처리해야 할 것**을 다룬다.
작업을 시작하기 전에 `CLAUDE.md`를 먼저 읽고, 이 문서로 "실제로는 이렇게 됐다"를 보정할 것.

---

## 1. 브랜치 구조 — 지금 뭘 체크아웃해야 하는가

```
main   — 팀원이 만든 초기 스캐폴드 + database.sql 초안만 있음 (거의 비어있음)
dev    — 어드민 API 전체 구현 완료 (지역/장소/코스/리워드/인증/에러처리/Swagger)
feat/api — dev 에서 분기, 유저(비관리자) API 전체 구현 (코스 조회/참가/스탬프/리워드수령/마이페이지)
```

**지금 작업은 전부 `feat/api`에서 진행 중이다.** `dev`는 `feat/api`의 부모이고, `main`은 사실상
초기 스캐폴드 상태로 멈춰 있다 (PR 준비되면 `dev` → `main`으로 머지될 것으로 추정, 확인 필요).

### ⚠️ 처리 안 된 이슈: `dev` 브랜치에 stash가 남아있음

`dev` 브랜치에 `git stash` 처리해둔 미커밋 변경사항이 있다 (`stash@{0}`):
```
- src/main/resources/application.properties (CORS origin을 로컬 IP로 임시 변경)
- src/main/resources/db/migration/V3__coupon_modification.sql (신규 파일 — rewards 에 data_string 컬럼 추가)
```

**중요**: `feat/api`는 이미 `V3__place_description.sql`을 쓰고 있다 (places.description 컬럼 추가).
`dev`의 stash에 있는 `V3__coupon_modification.sql`과 **파일명이 충돌**한다. 이 stash를 꺼내
커밋하려면 반드시 `V5__coupon_modification.sql`(또는 현재 최신 다음 번호)로 이름을 바꿔야 한다.
`git stash show -p stash@{0}`로 내용 확인 가능. 이 작업을 아무도 안 했으니 누군가 처리해야 함.

---

## 2. 스택 — `CLAUDE.md` 원문과 실제 구현이 다른 부분

`CLAUDE.md` 1절은 PostgreSQL 16 / Spring Boot 3.3.x 라고 적혀 있지만, **실제로는 처음부터
MySQL + Boot 4.1.1로 세팅되어 있었다** (팀원이 만든 최초 스캐폴드가 그랬음). 사용자에게 확인
후 "실제 코드 기준"으로 진행하기로 결정했고, `CLAUDE.md` 1절도 이미 "MySQL 8.4 LTS"로
고쳐져 있다 (Boot 버전 줄은 아직 3.3.x로 남아있지만 실제는 4.1.1).

| 항목 | CLAUDE.md 원문 | 실제 |
| --- | --- | --- |
| DB | PostgreSQL 16 | **MySQL 8.4 LTS** |
| Spring Boot | 3.3.x | **4.1.1** |
| Hibernate | 6 | **7.4.5.Final** (Boot 4.1.1이 끌고옴) |
| Jackson | (명시 안 됨, 암묵적으로 2.x 가정) | **Jackson 3** (`tools.jackson.*` 패키지, `com.fasterxml.jackson.databind`가 **아님**. `jackson-annotations`만 예외적으로 옛날 패키지 유지) |

**Boot 4.1.1 관련 함정들 (다음에 또 안 걸리려면 알아둘 것)**:
- `ObjectMapper`, `JsonNode` 등은 `tools.jackson.databind.*`에서 import. `com.fasterxml.jackson.databind.*`는 없음.
- `@JsonValue`/`@JsonCreator` 같은 애노테이션은 여전히 `com.fasterxml.jackson.annotation.*` (jackson-annotations 모듈은 구 패키지 유지).
- `spring-boot-starter-web` 이 아니라 `spring-boot-starter-webmvc`.
- 테스트 쪽 `@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` (구 패키지 아님).
- Auto-config 클래스 패키지도 세분화됨 (예: `org.springframework.boot.hibernate.autoconfigure.HibernateJpaConfiguration`).
- springdoc-openapi는 **3.x 라인**을 써야 함 (2.x는 Jackson 2 전제라 안 붙음).
- Testcontainers는 core가 **2.0.5**로 강제되는데, `testcontainers:mysql`/`testcontainers:junit-jupiter` 모듈은 아직 2.x가 안 나와서 **1.21.4로 명시 고정**해야 함 (build.gradle 참고). Gradle이 core만 2.0.5로 끌어올리고 나머지 모듈은 1.21.4로 맞물려 문제없이 동작 확인함.

**enum 값 케이스 컨벤션**: 전체 API가 `spring.jackson.property-naming-strategy=SNAKE_CASE`.
enum 값 자체는 기본적으로 Java 상수명 그대로(대문자, 예: `"OFFICIAL"`, `"PUBLISHED"`) 내려가는데,
**`user.Role`만 예외**로 `@JsonValue`/`@JsonCreator`를 달아서 소문자(`"organization"`, `"user"`)로
내려간다 — CLAUDE.md §5.1이 "프론트가 이 값으로 라우팅한다"고 명시했기 때문. 다른 enum까지
소문자로 바꾸고 싶다면 프론트와 확인 필요 (지금은 안 그렇게 되어 있음).

DB 컬럼 자체는 (Postgres가 아니라 MySQL이라) V1 마이그레이션에서 이미 소문자 ENUM/VARCHAR
값(`'official'`, `'active'` 등)으로 정의되어 있다. 엔티티들은 `common/persistence/LowerCaseEnumConverter`
라는 공용 AttributeConverter로 "Java는 대문자 상수, DB는 소문자 문자열"을 왕복 변환한다.
새 enum 컬럼을 추가할 때는 이 패턴을 그대로 따를 것 (컨버터 하나씩 만들어서 `@Converter(autoApply = true)`).

---

## 3. 지금까지 구현된 것

### 3-1. 어드민 API (`dev`, CLAUDE.md §5 전체 커버)
- 인증: `POST /admin/auth/register`(조직+관리자 동시 생성), `POST /auth/login`
- 지역 CRUD (`/admin/regions`) — N+1 없는 단일 JPQL projection
- 장소 CRUD + QR PNG 생성 (`/admin/places`, ZXing) — `qrcode_string`은 서버가 UUID로 강제 생성
- 리워드 CRUD (`/admin/rewards`)
- 코스 CRUD + 장소 구성 전량 교체 (`PUT /admin/courses/{id}/places`) + 통계 + 검수(승인/반려)
- `GlobalExceptionHandler` + `ErrorCode` — CLAUDE.md §6 에러 계약 전체 구현
- Swagger/OpenAPI (`/swagger-ui/index.html`, `/v3/api-docs`) — springdoc 3.1.1
- Flyway 시드 데이터 (`db/migration/dev/R__seed_dev.sql`, `dev` 프로파일에서만 적용)

### 3-2. 유저(비관리자) API (`feat/api`, CLAUDE.md에는 없던 새 스펙 — 프론트 요청으로 추가됨)
팀원(`kkjjkkll1111`)과 이어받아서 같이 만든 것들:

| 엔드포인트 | 설명 |
| --- | --- |
| `POST /auth/register` | 일반 사용자(Role.USER) 회원가입 |
| `GET /courses`, `GET /courses/{id}` | 공개 코스 목록/상세 (region_id/region_name, distance_meters, duration_minutes 포함) |
| `POST /courses/{courseId}/enrollments` | 코스 시작하기 (기존 있으면 그대로 반환, ABANDONED면 리셋 후 재시작) |
| `GET /enrollments/{id}` | 진행 상황 조회 (course/places[스탬프 여부 포함]/reward 전부 포함) |
| `POST /enrollments/{id}/abandon` | 코스 포기 (COMPLETE는 못 지움, ABANDONED는 멱등) |
| `POST /enrollments/{id}/stamps` | QR+GPS 방문 인증 (하버사인 거리 50m, 방문순서 검증) |
| `GET /users/me` | 내 프로필 |
| `GET /users/me/enrollments` | 내 코스 목록 |
| `POST /reward-claims` | 코스 완주 리워드 수령 |
| `POST /reward-claims/{claimId}/redeem` | 리워드 사용 처리 (status → "used") |
| `GET /users/me/reward-claims` | 내 리워드함 (course_id/course_title 포함) |

### 3-3. 이번 세션에서 고친 버그
- **`courses.view_count` 타입 버그**: 엔티티가 `Long`(BIGINT)인데 DB는 `INT UNSIGNED` — Hibernate
  `ddl-auto=validate`가 실제 MySQL에 붙였을 때만 걸러짐 (컴파일이나 H2로는 못 잡음). `Integer`로 수정.
- **Testcontainers 싱글톤 컨테이너 패턴**: `@Testcontainers` + `static @Container`를 여러 테스트
  클래스가 상속하면, JUnit5가 **테스트 클래스마다** 컨테이너를 start/stop 해서 한 클래스가 끝나며
  컨테이너를 내리면 다음 클래스가 죽은 포트로 접속을 시도해 깨짐. `static { MYSQL.start(); }`
  초기화 블록으로 JVM당 한 번만 띄우는 방식으로 교체함 (`IntegrationTestSupport` 참고, 위 3.md 인용).
- **코스 조회수 사용자별 unique 처리**: 새로고침마다 무한히 오르던 것을, `course_views`
  테이블(`V4__course_views.sql`)로 (course_id, user_id) 당 최초 조회에서만 카운트하도록 수정.
  익명 조회는 구분할 방법이 없어 매번 카운트됨 — 이건 판단이 필요해서 사용자에게 확인 후 진행.
- **포기(abandon) 후 재시작 안 되던 버그**: `course_enrollments`가 유저당 코스당 row 1개뿐이라,
  포기해도 재시작이 영원히 막혀 있었음. 같은 row를 ACTIVE로 리셋 + 기존 스탬프 삭제하는 방식으로
  수정 (사용자 확인 후 진행). `CourseEnrollment.startedAt`을 `@CreatedDate`(불변)에서 수동
  관리 필드로 바꿔서 재시작 시 갱신 가능하게 함.
- **`reward_claims.status`**: DB에 CHECK 제약이 없는 자유 문자열. 실제로 저장되는 값은
  `"claimed"`(수령 시)/`"used"`(사용 처리 시, 이번 세션에 추가) 두 가지뿐이고, `"expired"`는
  응답 시점에 `valid_until` 기준으로 계산만 되고 DB엔 저장 안 됨.

---

## 4. 아키텍처 패턴 (새 기능 추가할 때 따라야 할 관례)

- **조직 스코프**: 어드민 쪽 모든 조회/수정/삭제는 `findByIdAndOrganizationId(id, me.orgId())`
  패턴 강제. 다른 조직 리소스는 403이 아니라 **404** (존재 노출 방지).
- **소유권 검사 (유저 API)**: 마찬가지로 `findByIdAndUserId(id, me.userId())` 형태로 본인 것만
  조회되게 하고, 없으면 404 (다른 유저 리소스 존재 자체를 숨김).
- **동시성**: 상태를 바꾸는 곳(코스 시작/포기/스탬프/리워드 수령/재고 차감)은 전부
  `@Lock(LockModeType.PESSIMISTIC_WRITE)`로 대상 row를 잠근 `findXxxForUpdate` 리포지토리
  메서드를 씀. 재고 차감처럼 원자성이 중요한 곳은 `UPDATE ... WHERE stock > 0` 형태의
  `@Modifying` 조건부 갱신(`decrementStockIfAvailable`)으로 처리.
- **멱등성**: 이미 존재하는 리소스에 대한 재요청(코스 재시작, 리워드 재수령 등)은 에러 대신
  200으로 기존 상태를 그대로 반환. 새로 생성됐을 때만 201.
- **N+1 방지**: 목록 조회는 `@EntityGraph` 또는 JPQL `select new .../group by` projection.
  절대 리스트 순회하면서 개별 쿼리 날리지 말 것.
- **REQUIRES_NEW 트랜잭션**: "실패해도 무방한 부수효과"(조회수 중복 방지용 insert 같은 것)는
  별도 `@Transactional(propagation = REQUIRES_NEW)` 서비스로 분리해서, 그 안에서 유니크 제약
  위반이 나도 **호출자의 트랜잭션이 rollback-only로 오염되지 않게** 함 (`CourseViewService` 참고).
- **에러 코드**: 전부 `common/error/ErrorCode`에 등록. 상태 코드 400/401/403/404/409/500 외에
  **유저 API 쪽에서 422(Unprocessable Content)도 쓰기 시작함** (QR/GPS 검증 실패 등) — 이건
  CLAUDE.md §6 에러 표에 없던 것이라, 프론트가 이 상태코드를 실제로 처리하는지 확인 필요.
- **DTO**: 전부 record, `XxxResponse.from(entity)` 정적 팩토리. MapStruct 등 매핑 라이브러리 금지.
- **PUT의 "필드 없음" vs "필드가 null"** (코스 `reward_id` 연결 해제 등): `JsonNullable` 라이브러리는
  Jackson 2 전제라 Boot 4/Jackson 3에서 못 씀. 대신 컨트롤러가 `tools.jackson.databind.JsonNode`로
  원본 바디를 받아서 `body.has("reward_id")`로 직접 확인하고, 나머지 필드만 typed DTO로 변환하는
  방식을 씀 (`CourseController.update()`, `common/web/JsonNullableField` 참고).

---

## 5. 검증 상태 — 반드시 알아야 할 제약

**(2026-09-14 갱신) 현재 WSL 세션 환경에는 Docker 29.8.0이 정상 동작한다** (`docker version` 확인됨,
별도 `DOCKER_HOST` 원격 설정 없이 로컬 데몬 사용 가능). 이전 세션들에서는 샌드박스에 기본적으로
Docker가 없어서, 중간에 사용자가 한 번 `DOCKER_HOST=tcp://...:2375`로 원격 데몬을 열어줬을 때만
`./gradlew test`를 돌려 20/20 통과를 확인했고 (그 과정에서 `view_count` 타입 버그와 Testcontainers
싱글톤 패턴 버그를 실제로 잡음), 그 이후 세션들에서는 다시 연결이 끊겨서 컴파일/빌드
(`compileJava`, `compileTestJava`, `./gradlew build -x test`)까지만 확인하고 커밋했었다.

이제는 이 환경 제약이 없으므로 **다음 작업자는 커밋 전에 항상**:

```bash
./gradlew test
```
를 실제로 돌려서 확인할 것 (더 이상 "Docker 없어서 컴파일까지만 확인" 예외를 쓸 수 없음). 특히:
- `V3__place_description.sql`, `V4__course_views.sql` 마이그레이션이 실제로 깨끗하게 적용되는지
- `EnrollmentIntegrationTest`의 재시작 시나리오, `RewardClaimIntegrationTest`의 redeem 시나리오
- 동시성 테스트들 (`RewardClaimIntegrationTest`의 `runConcurrently` 기반 레이스 테스트)

### 5.1 (2026-09-14) 실제로 돌려본 결과 — 124개 중 2개 실패, 둘 다 테스트 코드 쪽 문제(프로덕션 버그 아님)

Docker 사용 가능해진 이 세션에서 `./gradlew test`를 처음으로 실제 실행함. 결과: 124 tests, 2 failed.
**아직 고치지 않은 상태** — 다음 작업자가 처리할 것.

1. `UserCourseIntegrationTest.listFiltersPublishedCoursesWithoutDuplicatesAndUsesFirstPlaceImage()`
   — `assertThat(item.size()).isEqualTo(6)`에서 `expected: 6 but was: 9`로 실패.
   원인: `UserCourseListItem`(`dto/user/UserCourseListItem.java`)에 이후 세션에서 프론트 요청으로
   `region_id`/`region_name`/`duration_minutes`가 추가됐는데(§9-2 유저 API 표 참고) 이 테스트의
   필드 개수 단언이 그대로 6으로 남아있었음. **API 동작 자체는 정상** — 단언값을 9로 갱신하거나,
   필드 개수 대신 존재해야 할 필드들을 개별로 검증하는 방식으로 바꿔야 함.
2. `RewardClaimIntegrationTest.claimsRewardAndReturnsItFromRewardBox()`
   — `org.hibernate.LazyInitializationException: Could not initialize proxy [Course#43] - no session`.
   원인: 테스트 74번째 줄 `enrollment.getCourse().getName()`이 `enrollment`를 생성한 트랜잭션이
   이미 끝난 뒤 lazy proxy를 초기화하려다 실패 (mockMvc 호출 자체는 200으로 정상 통과함, API 응답도
   정상). 테스트만 고치면 됨 — course를 미리 `courseRepository.findById`로 다시 조회해 이름을 얻거나,
   `enrollment(...)` 헬퍼가 만드는 course 참조를 테스트 메서드 지역변수로 따로 들고 있다가 그 이름을
   직접 쓰는 방식으로 수정.

두 실패 모두 **프로덕션 코드는 건드릴 필요 없고 테스트 코드만 수정하면 된다.**

---

## 6. CLAUDE.md 자체의 문제 (스펙 문서인데 오류가 있음)

- **§1 제목이 깨져 있음**: "## 1. 스택 / 버전" → "## 1. 스택 / 버"로 잘려있다 (누군가 실수로
  편집한 것으로 보임, 아직 안 고침).
- **§8 시드 데이터 설명이 자기모순**: "코스 6개(official 4, user 1, ai 1)"라고 해놓고 바로 아래
  "검수 대기 4개(ai 2, user 2)"라고 적어놨는데, user 1 + ai 1로는 검수 대기가 2개밖에 안 나옴.
  실제 시드는 검수 대기 4개 쪽을 기준으로 코스를 8개(official 4 + user 2 + ai 2) 채웠음
  (`R__seed_dev.sql` 커밋 메시지에 이 판단 기록해둠).

---

## 7. 로컬 실행

```bash
# 로컬 개발 (docker-compose 가 compose.yaml 의 MySQL 8.4 를 자동 기동)
./gradlew bootRun

# dev 프로파일 (시드 데이터 포함, 빈 DB에 최초 1회만 적용 권장 — 재적용하려면 볼륨 초기화)
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# 배포/운영 (docker-compose 자동기동 끄고 환경변수로 명시 주입)
SPRING_PROFILES_ACTIVE=prod DB_URL=... DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=... FRONTEND_ORIGIN=... ./gradlew bootRun

# 테스트 (Docker 필수 — Testcontainers 가 MySQL 8.4 컨테이너를 직접 띄움, H2 대체 금지)
./gradlew test
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## 8. 다음에 할 만한 것 (우선순위 순 아님, 필요한 것 골라서)

1. **`./gradlew test`를 실제로 돌려서 이번 세션 커밋들 검증** (가장 시급 — 위 5절 참고)
2. `dev` 브랜치의 stash 처리 (V3 파일명 충돌 리네이밍 포함)
3. `feat/api` → `dev` (또는 `main`) 머지 계획 수립 — 이 시점에 V3 파일명 충돌도 같이 해결
4. CLAUDE.md §1/§8 오류 수정
5. HTTP 422 사용을 프론트와 확정짓기 (§6 에러 표에 정식으로 추가할지)
6. `config/WebConfig`는 CLAUDE.md §2에 언급됐지만 아직 안 만듦 (지금까지 필요 없었음 — CORS는
   SecurityConfig 안에서 처리 중)

---

## 9. (2026-09-14) User 코스 추천 기능 구현 완료

일반 사용자가 조직의 Place를 골라 자신만의 코스(`type=USER`)를 만들어 **즉시 게시**하는 기능을 추가했다.
게시 즉시 리워드 없이 공개되고, 관리자가 기존 검수 큐에서 승인(보너스 리워드 부여)/반려(반려 시 `ARCHIVED`
처리해 공개 목록에서 숨김)할 수 있다.

**신규 엔드포인트**:
- `POST /courses` — 사용자 코스 생성 (로그인만 필요, 관리자 권한 불필요). `place_ids`는 최소 2개,
  배열 순서 그대로 `visit_order`가 된다. `course/UserCourseCommandService.java` 참고.
- `GET /organizations` — 조직 목록 (로그인만 필요). `organization/OrganizationController.java`.
- `GET /organizations/{id}/places` — 조직의 장소 목록, **`qrcode_string` 절대 미포함** (공개 API라
  스탬프 인증 우회 방지). `place/dto/PublicPlaceItem.java`.

**기존 로직 변경**:
- `CourseService.pending()`이 더 이상 `status=DRAFT`로 필터링하지 않고, "아직 `CourseReview` 행이
  없다"로 조건이 바뀌었다(`CourseRepository.findPendingReview`). 사용자 코스는 생성 즉시 `PUBLISHED`라
  기존 DRAFT 필터로는 검수 큐에 안 잡혔기 때문. **다른 기능이 이 pending() 결과에 `status=DRAFT`를
  암묵적으로 가정하고 있다면 깨질 수 있으니 확인할 것** (현재는 어드민 프론트가 이미 제네릭하게 처리해서
  문제없음을 확인함).
- `CourseService.reject()`가 이제 `CourseStatus.ARCHIVED`로 상태를 바꾼다 (이전엔 상태 불변). 이 결정은
  사용자에게 명시적으로 확인받은 것 — 반려된 사용자 코스가 계속 공개 목록에 남아있으면 안 된다는 판단.

**실제로 기동해서 검증함** (Docker/mysql 컨테이너 직접 띄워서, `dev` 프로파일 시드 데이터 기준):
회원가입→로그인→`GET /organizations`→`GET /organizations/{id}/places`(qrcode 미노출 확인)→
`POST /courses`(2곳, 즉시 공개 확인)→관리자 `GET /admin/courses/pending?type=USER`(뜨는지 확인)→
`approve`(보너스 리워드 연결 확인)/`reject`(공개 목록에서 사라짐 확인) 전부 실제 HTTP로 통과.
엣지케이스(장소 1개/중복/타 조직 장소/미존재 조직/비로그인)도 전부 의도한 에러코드로 응답함을 확인.

`./gradlew test` 132개 중 130개 통과 (신규 테스트 8개 전부 통과) — 나머지 2개 실패는 이 기능과 무관한
기존 stale 테스트(§5.1 참고, 아직 미수정).

`CLAUDE.md`에는 이 기능이 반영되어 있지 않다(원 스펙 문서라 임의로 덮어쓰지 않음) — 다음에 CLAUDE.md를
갱신할 일이 있으면 §5(엔드포인트 표)에 `POST /courses`, `GET /organizations`, `GET /organizations/{id}/places`를
추가할 것.
