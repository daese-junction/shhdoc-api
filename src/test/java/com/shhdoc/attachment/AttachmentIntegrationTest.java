package com.shhdoc.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shhdoc.TestcontainersConfiguration;
import com.shhdoc.storage.AttachmentStorage;
import com.shhdoc.upstage.Gateway;
import com.shhdoc.upstage.dto.MailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 첨부 업로드·조회 흐름. 스토리지는 대역으로 바꾼다.
 * CI 에는 MinIO 가 없고, 여기서 검증할 것은 권한·상태 규칙이지 S3 프로토콜이 아니다.
 * 서명 URL 이 실제로 동작하는지는 로컬에서 MinIO 를 띄워 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttachmentStorage storage;

    @MockitoBean
    private Gateway gateway;

    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() throws Exception {
        given(storage.presignUpload(anyString()))
                .willReturn(new AttachmentStorage.PresignedUpload("key-1", "http://storage.test/put", 900));
        given(storage.requireUploaded("key-1")).willReturn(2048L);
        given(storage.sha256("key-1")).willReturn("hash-1");
        given(storage.requireUploaded("key-2")).willReturn(4096L);
        given(storage.sha256("key-2")).willReturn("hash-2");
        given(storage.presignDownload(anyString(), anyString()))
                .willReturn(new AttachmentStorage.PresignedDownload("http://storage.test/get", 900));

        if (adminToken != null) {
            return;
        }
        mockMvc.perform(post("/companies").contentType(APPLICATION_JSON).content("""
                        {"companyName":"쉿닥","emailDomain":"attach-test.com",
                         "email":"admin@attach-test.com","password":"password123","name":"김대표"}
                        """))
                .andExpect(status().isCreated());
        adminToken = login("admin@attach-test.com");

        mockMvc.perform(post("/companies/members")
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"bob@attach-test.com","password":"password123","name":"박직원"}
                                """))
                .andExpect(status().isCreated());
        memberToken = login("bob@attach-test.com");
    }

    @Test
    void 업로드_URL_발급부터_등록과_관리자_열람까지() throws Exception {
        Long mailId = createDraft();

        mockMvc.perform(post("/emails/{id}/attachments/upload-url", mailId)
                        .header(AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"filename":"내부_설계도.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageKey").value("key-1"))
                .andExpect(jsonPath("$.uploadUrl").value("http://storage.test/put"));

        String body = mockMvc.perform(post("/emails/{id}/attachments", mailId)
                        .header(AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"storageKey":"key-1","filename":"내부_설계도.pdf"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("내부_설계도.pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(2048))
                .andExpect(jsonPath("$.scanStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Long attachmentId = ((Number) JsonPath.read(body, "$.id")).longValue();

        // 관리자는 승인 판단을 위해 같은 회사 첨부를 열 수 있다
        mockMvc.perform(get("/attachments/{id}/download-url", attachmentId)
                        .header(AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value("http://storage.test/get"));
    }

    /**
     * 등록이 201 을 주는 것만으로는 부족하다. 검사 요청은 커밋 이후 리스너에서 나가는데,
     * 거기서 터진 예외는 스프링이 삼켜서 응답에 아무 흔적이 남지 않는다.
     * 실제로 enqueue 까지 갔는지 확인해야 조용히 검사가 누락되는 걸 잡는다.
     */
    @Test
    void 첨부를_등록하면_검사가_요청된다() throws Exception {
        Long mailId = createDraft();

        mockMvc.perform(post("/emails/{id}/attachments", mailId)
                        .header(AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"storageKey":"key-2","filename":"계약서.pdf"}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<MailRequest> captor = ArgumentCaptor.forClass(MailRequest.class);
        verify(gateway).enqueue(captor.capture());
        assertThat(captor.getValue().senderAddress()).isEqualTo("bob@attach-test.com");
        assertThat(captor.getValue().attachments()).singleElement()
                .satisfies(a -> assertThat(a.storageKey()).isEqualTo("key-2"));
    }

    @Test
    void 발송한_메일에는_첨부를_붙일_수_없다() throws Exception {
        Long mailId = createDraft();
        mockMvc.perform(post("/emails/{id}/send", mailId).header(AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/emails/{id}/attachments/upload-url", mailId)
                        .header(AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"filename":"추가.pdf"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 남의_메일에는_첨부할_수_없다() throws Exception {
        Long mailId = createDraft();

        mockMvc.perform(post("/emails/{id}/attachments/upload-url", mailId)
                        .header(AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"filename":"남의메일.pdf"}
                                """))
                .andExpect(status().isNotFound());
    }

    private Long createDraft() throws Exception {
        String body = mockMvc.perform(post("/emails")
                        .header(AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"subject":"설계도","body":"확인",
                                 "recipients":[{"address":"partner@example.com","type":"TO"}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
