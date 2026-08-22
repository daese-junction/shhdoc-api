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

    @Test
    void mail과_분석결과를_MailContext로_조립한다() {
        MailRequest mail = new MailRequest(
                1, 100, "sender@a.com", 1, "제목", "본문",
                List.of(new Recipient("r1@b.com"), new Recipient("r2@b.com")),
                List.of()
        );
        List<SensitiveItem> sensitiveItems = List.of(new SensitiveItem("이름", "홍길동"));
        ExtractionResult extraction = new ExtractionResult(sensitiveItems, true, false, "CONFIDENTIAL");
        ClassificationResult classification = new ClassificationResult("payslip", 0.9);
        ParsedDocument parsed = new ParsedDocument(new ParsedContent("<h1>x</h1>", "", ""), List.of(), 1);
        DocumentAnalysisResult docResult = new DocumentAnalysisResult(parsed, classification, extraction);

        MailContext context = contextBuilder.build(mail, docResult);

        assertThat(context.senderAddress()).isEqualTo("sender@a.com");
        assertThat(context.recipientAddresses()).containsExactly("r1@b.com", "r2@b.com");
        assertThat(context.category()).isEqualTo("payslip");
        assertThat(context.sensitiveItems()).isEqualTo(sensitiveItems);
        assertThat(context.containsPersonalInfo()).isTrue();
        assertThat(context.containsFinancialInfo()).isFalse();
        assertThat(context.confidentialityMarking()).isEqualTo("CONFIDENTIAL");
    }
}
