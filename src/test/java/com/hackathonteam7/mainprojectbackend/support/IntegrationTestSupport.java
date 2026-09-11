package com.hackathonteam7.mainprojectbackend.support;

import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationMember;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationMemberRepository;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationMemberRole;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
import com.hackathonteam7.mainprojectbackend.security.JwtTokenProvider;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;

/**
 * §7: "DB 는 Testcontainers ... H2 로 대체하지 않는다(FK RESTRICT 동작이 달라진다)."
 * docker-compose 자동 기동(application.properties 기본값)은 여기서 끄고, 컨테이너의 접속정보를 직접 주입한다.
 *
 * "싱글톤 컨테이너" 패턴: {@code @Testcontainers}+{@code @Container} 를 쓰지 않는다 — 그 조합은
 * static 필드를 상속받는 각 테스트 클래스마다 JUnit5 확장이 컨테이너를 start/stop 하므로, 한 클래스가
 * 끝나며 컨테이너를 내리면 다음 클래스는 캐시된(오래된 포트를 가리키는) Spring 컨텍스트로 연결을 시도하다
 * "Connection refused" 로 깨진다. 대신 static 초기화 블록에서 JVM 당 한 번만 기동한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("main_project_backend_test")
            .withUsername("app")
            .withPassword("app");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.docker.compose.enabled", () -> "false");
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected OrganizationRepository organizationRepository;

    @Autowired
    protected OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    protected Organization organization;
    protected User adminUser;
    protected String accessToken;

    @BeforeEach
    void setUpOrganizationAndAdmin() {
        organization = organizationRepository.save(
                Organization.builder().name("테스트 재단").type(OrganizationType.FACILITY).build());
        adminUser = userRepository.save(
                User.builder()
                        .name("관리자")
                        .email("admin-" + System.nanoTime() + "@example.com")
                        .password(passwordEncoder.encode("password1234"))
                        .role(Role.ORGANIZATION)
                        .build());
        organizationMemberRepository.save(
                OrganizationMember.builder().organization(organization).user(adminUser).role(OrganizationMemberRole.OWNER).build());
        accessToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(), adminUser.getRole(), organization.getId());
    }
}
