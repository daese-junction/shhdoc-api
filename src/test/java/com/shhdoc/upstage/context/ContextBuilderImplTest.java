package com.shhdoc.upstage.context;

import com.shhdoc.company.Company;
import com.shhdoc.company.CompanyRepository;
import com.shhdoc.policy.entity.RecipientDomain;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.repository.RecipientDomainRepository;
import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.pipeline.classify.ClassificationResult;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import com.shhdoc.upstage.pipeline.parse.ParsedContent;
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextBuilderImplTest {

    private static final Long COMPANY_ID = 100L;

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private RecipientDomainRepository recipientDomainRepository;

    private ContextBuilderImpl contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new ContextBuilderImpl(companyRepository, recipientDomainRepository);

        Company company = Mockito.mock(Company.class);
        lenient().when(company.getEmailDomain()).thenReturn("company.com");
        lenient().when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        lenient().when(recipientDomainRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
    }

    private DocumentAnalysisResult docResult() {
        ExtractionResult extraction = new ExtractionResult(List.of("PERSONAL"), "CONFIDENTIAL", "대외비");
        ClassificationResult classification = new ClassificationResult("payslip", 0.9);
        ParsedDocument parsed = new ParsedDocument(new ParsedContent("<h1>x</h1>", "", ""), List.of(), 1);
        return new DocumentAnalysisResult(parsed, classification, extraction);
    }

    private MailRequest mailTo(String... recipientAddresses) {
        List<Recipient> recipients = List.of(recipientAddresses).stream().map(Recipient::new).toList();
        return new MailRequest(1L, COMPANY_ID, "sender@company.com", 1L, "제목", "본문", recipients, List.of());
    }

    @Test
    void mail과_분석결과를_MailContext로_조립한다() {
        MailContext context = contextBuilder.build(mailTo("r1@b.com", "r2@b.com"), docResult());

        assertThat(context.senderAddress()).isEqualTo("sender@company.com");
        assertThat(context.recipientAddresses()).containsExactly("r1@b.com", "r2@b.com");
        assertThat(context.category()).isEqualTo("payslip");
        assertThat(context.sensitiveTypeCodes()).containsExactly("PERSONAL");
        assertThat(context.classification()).isEqualTo("CONFIDENTIAL");
        assertThat(context.confidentialityMarking()).isEqualTo("대외비");
    }

    @Test
    void 수신자_도메인이_회사도메인과_같으면_internal이다() {
        MailContext context = contextBuilder.build(mailTo("colleague@company.com"), docResult());

        assertThat(context.recipientType()).isEqualTo("internal");
    }

    @Test
    void 등록된_파트너_도메인이면_partner다() {
        RecipientDomain partnerDomain = Mockito.mock(RecipientDomain.class);
        when(partnerDomain.getDomain()).thenReturn("partner.com");
        when(partnerDomain.getScope()).thenReturn(RecipientScope.PARTNER);
        when(recipientDomainRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of(partnerDomain));

        MailContext context = contextBuilder.build(mailTo("r@partner.com"), docResult());

        assertThat(context.recipientType()).isEqualTo("partner");
    }

    @Test
    void 등록된_개인메일_도메인이면_personal_email이다() {
        RecipientDomain personalDomain = Mockito.mock(RecipientDomain.class);
        when(personalDomain.getDomain()).thenReturn("gmail.com");
        when(personalDomain.getScope()).thenReturn(RecipientScope.PERSONAL_EMAIL);
        when(recipientDomainRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of(personalDomain));

        MailContext context = contextBuilder.build(mailTo("r@gmail.com"), docResult());

        assertThat(context.recipientType()).isEqualTo("personal_email");
    }

    @Test
    void 등록안된_도메인이면_external이다() {
        MailContext context = contextBuilder.build(mailTo("r@unknown.com"), docResult());

        assertThat(context.recipientType()).isEqualTo("external");
    }

    @Test
    void 수신자가_여러명이면_가장_위험한_유형으로_대표한다() {
        MailContext context = contextBuilder.build(mailTo("colleague@company.com", "r@unknown.com"), docResult());

        assertThat(context.recipientType()).isEqualTo("external");
    }
}
