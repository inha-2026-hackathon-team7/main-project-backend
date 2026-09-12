package com.hackathonteam7.mainprojectbackend.auth;

import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import com.hackathonteam7.mainprojectbackend.user.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAuthControllerTest extends IntegrationTestSupport {

    @Autowired ObjectMapper objectMapper;

    @Test
    void registerThenLoginUsesExactContractAndCannotEscalateRole() throws Exception {
        String email = uniqueEmail();
        String password = "  my travel passphrase  ";
        long organizationsBefore = organizationRepository.count();
        long membershipsBefore = organizationMemberRepository.count();
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "  여행자  ", "email", "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ",
                "password", password, "role", "organization", "organization_id", organization.getId()));

        var response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("여행자"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.user_id").isNumber())
                .andReturn().getResponse();
        var json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.size()).isEqualTo(3);
        var user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getPassword()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, user.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(password.strip(), user.getPassword())).isFalse();
        assertThat(organizationRepository.count()).isEqualTo(organizationsBefore);
        assertThat(organizationMemberRepository.count()).isEqualTo(membershipsBefore);

        var login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "  " + email.toUpperCase(java.util.Locale.ROOT) + " ", "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("user"))
                .andReturn().getResponse();
        String token = objectMapper.readTree(login.getContentAsString()).get("access_token").asText();
        mockMvc.perform(get("/admin/regions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void existingAdminEmailAlsoBlocksGeneralRegistration() throws Exception {
        register(adminUser.getEmail().toUpperCase(java.util.Locale.ROOT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICATED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void displayNamesMayRepeat() throws Exception {
        register(uniqueEmail()).andExpect(status().isCreated());
        register(uniqueEmail()).andExpect(status().isCreated());
    }

    @Test
    void invalidRequestsReturnDocumentedErrorWithoutSavingUsers() throws Exception {
        long before = userRepository.count();
        for (String body : List.of("{}", "null", "{", "")) {
            var response = mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andReturn().getResponse();
            assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(3);
        }
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "여행자", "email", uniqueEmail(), "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("password"));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void concurrentRegistrationCreatesOneUserAndReturns409ForOthers() throws Exception {
        String email = uniqueEmail();
        int requests = 6;
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(requests)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent start timed out");
                    }
                    var response = register(email).andReturn().getResponse();
                    if (response.getStatus() == 409) {
                        assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
                                .isEqualTo("EMAIL_DUPLICATED");
                    }
                    return response.getStatus();
                }));
            }
            try {
                assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
            assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(requests - 1);
        }
        assertThat(userRepository.findAll().stream().filter(user -> user.getEmail().equals(email)).count()).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions register(String email) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "name", "여행자", "email", email, "password", "my travel passphrase"))));
    }

    private String uniqueEmail() {
        return "traveler-" + UUID.randomUUID() + "@example.com";
    }
}
