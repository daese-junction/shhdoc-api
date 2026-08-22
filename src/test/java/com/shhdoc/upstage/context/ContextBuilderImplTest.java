package com.shhdoc.upstage.context;

import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.pipeline.classify.ClassificationResult;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import com.shhdoc.upstage.pipeline.extract.SensitiveItem;
import com.shhdoc.upstage.pipeline.parse.ParsedContent;
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextBuilderImplTest {

    private final ContextBuilderImpl contextBuilder = new ContextBuilderImpl();

    private DocumentAnalysisResult docResult() {
        List<SensitiveItem> sensitiveItems = List.of(new SensitiveItem("이름", "홍길동"));
        ExtractionResult extraction = new ExtractionResult(sensitiveItems, true, false, "CONFIDENTIAL");
        ClassificationResult classification = new ClassificationResult("payslip", 0.9);
        ParsedDocument parsed = new ParsedDocument(new ParsedContent("<h1>x</h1>", "", ""), List.of(), 1);
        return new DocumentAnalysisResult(parsed, classification, extraction);
    }

    @Test
    void mail과_분석결과를_MailContext로_조립한다() {
        MailRequest mail = new MailRequest(
                1L, 100L, "sender@a.com", 1L, "제목", "본문",
                List.of(new Recipient("r1@b.com"), new Recipient("r2@b.com")),
                List.of()
        );

        MailContext context = contextBuilder.build(mail, docResult());

        assertThat(context.senderAddress()).isEqualTo("sender@a.com");
        assertThat(context.recipientAddresses()).containsExactly("r1@b.com", "r2@b.com");
        assertThat(context.category()).isEqualTo("payslip");
        assertThat(context.sensitiveItems()).hasSize(1);
        assertThat(context.containsPersonalInfo()).isTrue();
        assertThat(context.containsFinancialInfo()).isFalse();
        assertThat(context.confidentialityMarking()).isEqualTo("CONFIDENTIAL");
    }

    @Test
    void 발신자와_수신자_도메인이_다르면_external이다() {
        MailRequest mail = new MailRequest(
                1L, 100L, "sender@a.com", 1L, "제목", "본문",
                List.of(new Recipient("r1@b.com")),
                List.of()
        );

        MailContext context = contextBuilder.build(mail, docResult());

        assertThat(context.recipientType()).isEqualTo("external");
    }

    @Test
    void 발신자와_모든_수신자_도메인이_같으면_internal이다() {
        MailRequest mail = new MailRequest(
                1L, 100L, "sender@a.com", 1L, "제목", "본문",
                List.of(new Recipient("r1@a.com"), new Recipient("r2@a.com")),
                List.of()
        );

        MailContext context = contextBuilder.build(mail, docResult());

        assertThat(context.recipientType()).isEqualTo("internal");
    }

    @Test
    void 수신자_중_하나라도_다른_도메인이면_external이다() {
        MailRequest mail = new MailRequest(
                1L, 100L, "sender@a.com", 1L, "제목", "본문",
                List.of(new Recipient("r1@a.com"), new Recipient("r2@b.com")),
                List.of()
        );

        MailContext context = contextBuilder.build(mail, docResult());

        assertThat(context.recipientType()).isEqualTo("external");
    }
}
