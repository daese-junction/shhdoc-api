package com.shhdoc.upstage.document;

import com.shhdoc.upstage.pipeline.DocumentFile;
import com.shhdoc.upstage.pipeline.classify.ClassificationResult;
import com.shhdoc.upstage.pipeline.classify.DocumentClassifier;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import com.shhdoc.upstage.pipeline.extract.InformationExtractor;
import com.shhdoc.upstage.pipeline.parse.DocumentParser;
import com.shhdoc.upstage.pipeline.parse.ParsedContent;
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAnalyzerImplTest {

    @Mock
    private DocumentParser documentParser;
    @Mock
    private DocumentClassifier documentClassifier;
    @Mock
    private InformationExtractor informationExtractor;

    private DocumentAnalyzerImpl documentAnalyzer;

    @BeforeEach
    void setUp() {
        documentAnalyzer = new DocumentAnalyzerImpl(documentParser, documentClassifier, informationExtractor);
    }

    @Test
    void parse_classify_extract_결과를_하나로_합친다() {
        DocumentFile file = new DocumentFile("test.pdf", "dummy".getBytes());
        ParsedDocument parsed = new ParsedDocument(new ParsedContent("<h1>x</h1>", "", ""), List.of(), 1);
        ClassificationResult classification = new ClassificationResult("payslip", 0.9);
        ExtractionResult extraction = new ExtractionResult(List.of(), true, false, "");

        when(documentParser.parse(file)).thenReturn(parsed);
        when(documentClassifier.classify(any(), anyList())).thenReturn(classification);
        when(informationExtractor.extract(file)).thenReturn(extraction);

        DocumentAnalysisResult result = documentAnalyzer.analyze(file);

        assertThat(result.parsed()).isSameAs(parsed);
        assertThat(result.classification()).isSameAs(classification);
        assertThat(result.extraction()).isSameAs(extraction);
    }
}
