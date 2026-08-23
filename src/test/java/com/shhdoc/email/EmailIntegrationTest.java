package com.shhdoc.email;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shhdoc.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** 초안 → 차단 첨부로 보류 → 관리자 대기열 → 승인 까지 한 번에 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
// 회사·계정은 한 번만 만든다. 기본(메서드마다 새 인스턴스)이면 두 번째부터 도메인 중복 409가 난다.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private com.shhdoc.attachment.AttachmentRepository attachmentRepository;

    private String adminToken;
    private String memberToken;

    /**
     * 승인 대기로 보내려면 차단 판정이 난 첨부가 있어야 한다 — 승인 트리거는 수신자가 아니라
     * 첨부 판정이다. 검사 파이프라인(스토리지 + upstage)을 태우는 대신 결과만 직접 심는다.
     */
    private void attachBlocked(Long emailId) {
        Email email = emailRepository.findById(emailId).orElseThrow();
        com.shhdoc.attachment.Attachment attachment = new com.shhdoc.attachment.Attachment(
                email, "설계도.pdf", 1024L, "key-" + emailId, "hash-" + emailId);
        attachment.recordVerdict(com.shhdoc.attachment.Verdict.BLOCKED, "내부 설계도로 판단됨");
        attachmentRepository.save(attachment);
    }

    @BeforeEach
    void setUp() throws Exception {
        if (adminToken != null) {
            return;
        }
        mockMvc.perform(post("/companies")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"companyName":"쉿닥","emailDomain":"mail-test.com",
                                 "email":"admin@mail-test.com","password":"password123","name":"김대표"}
                                """))
                .andExpect(status().isCreated());

        adminToken = login("admin@mail-test.com");

        mockMvc.perform(post("/companies/members")
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"bob@mail-test.com","password":"password123","name":"박직원"}
                                """))
                .andExpect(status().isCreated());

        memberToken = login("bob@mail-test.com");
    }

    @Test
    void 차단_첨부가_있으면_관리자_승인을_거쳐야_나간다() throws Exception {
        Long emailId = createDraft(memberToken, "partner@example.com");
        attachBlocked(emailId);

        // 발송 시도 → 차단 판정 첨부가 있어 보류
        mockMvc.perform(post("/emails/{id}/send", emailId).header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.sentAt").doesNotExist());

        // 관리자 대기열에 뜬다
        mockMvc.perform(get("/admin/emails").header(AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + emailId + ")]").exists());

        // 승인하면 발송된다
        mockMvc.perform(post("/admin/emails/{id}/approve", emailId)
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"note":"고객사 계약서라 허용"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").isNotEmpty());

        // 발신자에게도 결과와 사유가 보인다
        mockMvc.perform(get("/emails/{id}", emailId).header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.reviewNote").value("고객사 계약서라 허용"));
    }

    @Test
    void 사내끼리는_승인없이_바로_발송된다() throws Exception {
        Long emailId = createDraft(memberToken, "admin@mail-test.com");

        mockMvc.perform(post("/emails/{id}/send", emailId).header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    /**
     * 사외 수신자라는 이유만으로는 더 이상 막지 않는다. 사외 여부는 첨부를 검사할 때
     * recipientType 으로 정책에 반영되고, 위험하면 그때 차단 판정이 찍힌다.
     */
    @Test
    void 사외라도_걸리는_첨부가_없으면_바로_발송된다() throws Exception {
        Long emailId = createDraft(memberToken, "partner@example.com");

        mockMvc.perform(post("/emails/{id}/send", emailId).header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").isNotEmpty());
    }

    @Test
    void 일반_직원은_승인_대기열에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/admin/emails").header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 남의_메일은_보이지도_발송되지도_않는다() throws Exception {
        Long emailId = createDraft(memberToken, "partner@example.com");

        mockMvc.perform(get("/emails/{id}", emailId).header(AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/emails/{id}/send", emailId).header(AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 거절하면_사유가_발신자에게_보인다() throws Exception {
        Long emailId = createDraft(memberToken, "partner@example.com");
        attachBlocked(emailId);
        mockMvc.perform(post("/emails/{id}/send", emailId).header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk());

        // 사유 없이 거절은 막힌다
        mockMvc.perform(post("/admin/emails/{id}/reject", emailId)
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"note":"  "}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/admin/emails/{id}/reject", emailId)
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"note":"내부 설계도라 발송 불가"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/emails/{id}", emailId).header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewNote").value("내부 설계도라 발송 불가"));
    }

    private Long createDraft(String token, String recipient) throws Exception {
        String body = mockMvc.perform(post("/emails")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"subject":"내부 설계도","body":"확인 부탁드립니다.",
                                 "recipients":[{"address":"%s","type":"TO"}]}
                                """.formatted(recipient)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
