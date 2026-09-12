package com.hackathonteam7.mainprojectbackend.auth;

import com.hackathonteam7.mainprojectbackend.config.SecurityConfig;
import com.hackathonteam7.mainprojectbackend.security.JwtAuthenticationFilter;
import com.hackathonteam7.mainprojectbackend.security.JwtTokenProvider;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 MVC/보안/가입 Service를 실행하며 DB 경계만 대체한다. MySQL 통합 테스트는 별도로 유지한다. */
@WebMvcTest(UserAuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, UserRegistrationService.class})
class UserRegistrationWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    private static final String BODY = """
            {"name":"  여행자  ","email":"  USER@EXAMPLE.COM  ",
             "password":"  my travel passphrase  ","role":"organization","organization_id":1}
            """;

    @Test
    void anonymousSignupUsesExactResponseAndStoresOnlyNormalUserWithHash() throws Exception {
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });
        var response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_id").value(42))
                .andExpect(jsonPath("$.name").value("여행자"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andReturn().getResponse();
        assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(3);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("  my travel passphrase  ", saved.getValue().getPassword())).isTrue();
        assertThat(passwordEncoder.matches("my travel passphrase", saved.getValue().getPassword())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{", "{}",
            "{\"name\":\"여행자\",\"email\":\"user@example.com\",\"password\":\"short\"}"})
    void invalidBodyHasOnlyDocumentedErrorFieldsAndDoesNotReachDatabase(String body) throws Exception {
        var response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details").isArray())
                .andReturn().getResponse();
        assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(3);
        verifyNoInteractions(userRepository);
    }

    @Test
    void duplicateEmailFromPrecheckUsesDocumented409() throws Exception {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        assertDuplicateResponse();
    }

    @Test
    void duplicateEmailFromDatabaseUsesSameDocumented409() throws Exception {
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate", new ConstraintViolationException("duplicate", new SQLException(), "users.uq_users_email")));
        assertDuplicateResponse();
    }

    @Test
    void signupPermissionDoesNotOpenOtherMethodsOrAdminRoutes() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        String token = jwtTokenProvider.generateAccessToken(42L, "user@example.com", Role.USER, null);
        mockMvc.perform(get("/admin/regions").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private void assertDuplicateResponse() throws Exception {
        var response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICATED"))
                .andExpect(jsonPath("$.details").isArray())
                .andReturn().getResponse();
        assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(3);
    }
}
