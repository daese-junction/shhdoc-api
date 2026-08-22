package com.shhdoc.upstage.context;

import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import org.springframework.stereotype.Component;

@Component
public class ContextBuilderImpl implements ContextBuilder {

    @Override
    public MailContext build(MailRequest mail, DocumentAnalysisResult docResult) {
        ExtractionResult extraction = docResult.extraction();

        return new MailContext(
                mail.senderAddress(),
                mail.recipients().stream().map(Recipient::address).toList(),
                docResult.classification().category(),
                extraction.sensitiveItems(),
                extraction.containsPersonalInfo(),
                extraction.containsFinancialInfo(),
                extraction.confidentialityMarking()
        );
    }
}
