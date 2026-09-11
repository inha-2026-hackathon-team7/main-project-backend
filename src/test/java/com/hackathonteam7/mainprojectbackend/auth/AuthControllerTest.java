package com.hackathonteam7.mainprojectbackend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthControllerTest extends IntegrationTestSupport {

    @Test
    void register_success() throws Exception {
        String body = """
                {
                  "organization_name": "성수문화재단",
                  "organization_type": "FACILITY",
                  "admin_name": "김성수",
                  "admin_email": "new-admin@example.com",
                  "admin_password": "password1234"
                }
                """;

        mockMvc.perform(post("/admin/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organization_id").exists())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String body = """
                {
                  "organization_name": "중복 재단",
                  "organization_type": "FACILITY",
                  "admin_name": "관리자",
                  "admin_email": "%s",
                  "admin_password": "password1234"
                }
                """.formatted(adminUser.getEmail());

        mockMvc.perform(post("/admin/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICATED"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String body = """
                {"email": "%s", "password": "wrong-password"}
                """.formatted(adminUser.getEmail());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void login_success_returnsLowercaseRole() throws Exception {
        String body = """
                {"email": "%s", "password": "password1234"}
                """.formatted(adminUser.getEmail());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("organization"));
    }

    @Test
    void accessAdminEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/regions"))
                .andExpect(status().isUnauthorized());
    }
}
