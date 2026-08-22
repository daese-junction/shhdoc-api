package com.shhdoc.upstage.context;

import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextBuilderImpl implements ContextBuilder {

    private static final String INTERNAL = "internal";
    private static final String EXTERNAL = "external";

    @Override
    public MailContext build(MailRequest mail, DocumentAnalysisResult docResult) {
        ExtractionResult extraction = docResult.extraction();
        List<String> recipientAddresses = mail.recipients().stream().map(Recipient::address).toList();

        return new MailContext(
                mail.senderAddress(),
                recipientAddresses,
                resolveRecipientType(mail.senderAddress(), recipientAddresses),
                docResult.classification().category(),
                extraction.sensitiveItems(),
                extraction.containsPersonalInfo(),
                extraction.containsFinancialInfo(),
                extraction.confidentialityMarking()
        );
    }

    /**
     * 발신자와 모든 수신자의 도메인이 같으면 "internal", 아니면 "external".
     * 승인된 파트너/개인메일처럼 더 세분화된 유형은 아직 회사별 등록 도메인 데이터를
     * 조회하지 않아서 구분 못 하고 전부 "external"로 뭉뚱그린다 (Stage B에서 보강).
     */
    private String resolveRecipientType(String senderAddress, List<String> recipientAddresses) {
        String senderDomain = domainOf(senderAddress);
        boolean allInternal = !recipientAddresses.isEmpty()
                && recipientAddresses.stream().allMatch(address -> senderDomain.equals(domainOf(address)));
        return allInternal ? INTERNAL : EXTERNAL;
    }

    private String domainOf(String address) {
        int at = address.indexOf('@');
        return at < 0 ? "" : address.substring(at + 1).toLowerCase();
    }
}
