package com.shhdoc.auth;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shhdoc.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** 대표자가 회사를 만들고 직원을 추가하는 핵심 플로우 하나. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 회사_생성부터_직원_추가와_권한_차단까지() throws Exception {
        mockMvc.perform(post("/companies")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"companyName":"쉿닥","emailDomain":"shhdoc.com",
                                 "email":"alice@shhdoc.com","password":"password123","name":"alice"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company.emailDomain").value("shhdoc.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        String adminToken = login("alice@shhdoc.com", "password123");

        mockMvc.perform(get("/auth/me").header(AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@shhdoc.com"))
                .andExpect(jsonPath("$.company.name").value("쉿닥"));

        // 회사 도메인이 아닌 이메일은 관리자라도 추가할 수 없다
        mockMvc.perform(post("/companies/members")
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"bob@gmail.com","password":"password123","name":"bob"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/companies/members")
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"bob@shhdoc.com","password":"password123","name":"bob"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));

        // 추가된 직원은 로그인은 되지만 직원을 추가할 수는 없다
        String memberToken = login("bob@shhdoc.com", "password123");

        mockMvc.perform(post("/companies/members")
                        .header(AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"carol@shhdoc.com","password":"password123","name":"carol"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void swagger_문서에_설명과_bearer_인증이_실린다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.summary").value("로그인"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.description").isNotEmpty())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses.401.description").isNotEmpty())
                // 로그인은 공개 API라 자물쇠가 없어야 한다 (빈 배열 = 전역 인증 요구를 덮어씀)
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/companies/members'].post.summary")
                        .value("직원 계정 추가 (ADMIN 전용)"))
                // 직원 추가는 전역 인증 요구(루트 security)를 그대로 상속한다
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/companies/members'].post.security").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AddMemberRequest.properties.email.description")
                        .isNotEmpty());
    }

    @Test
    void 토큰_없이_보호된_API를_부르면_401() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/auth/me").header(AUTHORIZATION, "Bearer 아무거나"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
