package com.shhdoc.upstage.document;

import com.shhdoc.policy.entity.DocumentType;
import com.shhdoc.policy.entity.SensitiveInfoType;
import com.shhdoc.policy.repository.DocumentTypeRepository;
import com.shhdoc.policy.repository.SensitiveInfoTypeRepository;
import com.shhdoc.upstage.pipeline.DocumentFile;
import com.shhdoc.upstage.pipeline.classify.ClassificationResult;
import com.shhdoc.upstage.pipeline.classify.DefaultDocumentCategories;
import com.shhdoc.upstage.pipeline.classify.DocumentCategory;
import com.shhdoc.upstage.pipeline.classify.DocumentClassifier;
import com.shhdoc.upstage.pipeline.extract.DefaultSensitiveInfoTypes;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import com.shhdoc.upstage.pipeline.extract.InformationExtractor;
import com.shhdoc.upstage.pipeline.extract.SensitiveInfoCategory;
import com.shhdoc.upstage.pipeline.parse.DocumentParser;
import com.shhdoc.upstage.pipeline.parse.ParsedContent;
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAnalyzerImplTest {

    private static final Long COMPANY_ID = 100L;

    @Mock
    private DocumentParser documentParser;
    @Mock
    private DocumentClassifier documentClassifier;
    @Mock
    private InformationExtractor informationExtractor;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private SensitiveInfoTypeRepository sensitiveInfoTypeRepository;

    private DocumentAnalyzerImpl documentAnalyzer;

    @BeforeEach
    void setUp() {
        documentAnalyzer = new DocumentAnalyzerImpl(
                documentParser, documentClassifier, informationExtractor,
                documentTypeRepository, sensitiveInfoTypeRepository);
    }

    @Test
    void parse_classify_extract_결과를_하나로_합친다() {
        DocumentFile file = new DocumentFile("test.pdf", "dummy".getBytes());
        ParsedDocument parsed = new ParsedDocument(new ParsedContent("<h1>x</h1>", "", ""), List.of(), 1);
        ClassificationResult classification = new ClassificationResult("payslip", 0.9);
        ExtractionResult extraction = new ExtractionResult(List.of(), "CONFIDENTIAL", "");
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());

        when(documentParser.parse(file)).thenReturn(parsed);
        when(documentClassifier.classify(any(), anyList())).thenReturn(classification);
        when(informationExtractor.extract(any(), anyList())).thenReturn(extraction);

        DocumentAnalysisResult result = documentAnalyzer.analyze(file, COMPANY_ID);

        assertThat(result.parsed()).isSameAs(parsed);
        assertThat(result.classification()).isSameAs(classification);
        assertThat(result.extraction()).isSameAs(extraction);
    }

    @Test
    void 회사에_등록된_문서유형이_없으면_기본taxonomy로_분류요청한다() {
        DocumentFile file = new DocumentFile("test.pdf", "dummy".getBytes());
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(documentParser.parse(file)).thenReturn(new ParsedDocument(new ParsedContent("", "", ""), List.of(), 1));
        when(informationExtractor.extract(any(), anyList())).thenReturn(new ExtractionResult(List.of(), null, ""));
        ArgumentCaptor<List<DocumentCategory>> captor = ArgumentCaptor.forClass(List.class);
        when(documentClassifier.classify(any(), captor.capture())).thenReturn(new ClassificationResult("general", 1.0));

        documentAnalyzer.analyze(file, COMPANY_ID);

        assertThat(captor.getValue()).isEqualTo(DefaultDocumentCategories.ALL);
    }

    @Test
    void 회사에_등록된_문서유형이_있으면_그걸로_분류요청한다() {
        DocumentFile file = new DocumentFile("test.pdf", "dummy".getBytes());
        DocumentType documentType = Mockito.mock(DocumentType.class);
        when(documentType.getCode()).thenReturn("PAYROLL");
        when(documentType.getName()).thenReturn("급여명세서");
        when(documentType.getDescription()).thenReturn("급여 지급/공제 내역 문서");
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of(documentType));
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(documentParser.parse(file)).thenReturn(new ParsedDocument(new ParsedContent("", "", ""), List.of(), 1));
        when(informationExtractor.extract(any(), anyList())).thenReturn(new ExtractionResult(List.of(), null, ""));
        ArgumentCaptor<List<DocumentCategory>> captor = ArgumentCaptor.forClass(List.class);
        when(documentClassifier.classify(any(), captor.capture())).thenReturn(new ClassificationResult("PAYROLL", 1.0));

        documentAnalyzer.analyze(file, COMPANY_ID);

        assertThat(captor.getValue()).containsExactly(new DocumentCategory("PAYROLL", "급여명세서 - 급여 지급/공제 내역 문서"));
        verify(documentClassifier).classify(file, captor.getValue());
    }

    @Test
    void 회사에_등록된_민감정보유형이_없으면_기본값으로_추출요청한다() {
        DocumentFile file = new DocumentFile("test.pdf", "dummy".getBytes());
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(documentParser.parse(file)).thenReturn(new ParsedDocument(new ParsedContent("", "", ""), List.of(), 1));
        when(documentClassifier.classify(any(), anyList())).thenReturn(new ClassificationResult("general", 1.0));
        ArgumentCaptor<List<SensitiveInfoCategory>> captor = ArgumentCaptor.forClass(List.class);
        when(informationExtractor.extract(any(), captor.capture())).thenReturn(new ExtractionResult(List.of(), null, ""));

        documentAnalyzer.analyze(file, COMPANY_ID);

        assertThat(captor.getValue()).isEqualTo(DefaultSensitiveInfoTypes.ALL);
    }

    @Test
    void 회사에_등록된_민감정보유형이_있으면_그걸로_추출요청한다() {
        DocumentFile file = new DocumentFile("test.pdf", "dummy".getBytes());
        SensitiveInfoType sensitiveInfoType = Mockito.mock(SensitiveInfoType.class);
        when(sensitiveInfoType.getCode()).thenReturn("CREDENTIAL");
        when(sensitiveInfoType.getName()).thenReturn("인증정보");
        when(sensitiveInfoType.getDescription()).thenReturn("비밀번호, API 키 등");
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of(sensitiveInfoType));
        when(documentParser.parse(file)).thenReturn(new ParsedDocument(new ParsedContent("", "", ""), List.of(), 1));
        when(documentClassifier.classify(any(), anyList())).thenReturn(new ClassificationResult("general", 1.0));
        ArgumentCaptor<List<SensitiveInfoCategory>> captor = ArgumentCaptor.forClass(List.class);
        when(informationExtractor.extract(any(), captor.capture())).thenReturn(new ExtractionResult(List.of(), null, ""));

        documentAnalyzer.analyze(file, COMPANY_ID);

        assertThat(captor.getValue()).containsExactly(new SensitiveInfoCategory("CREDENTIAL", "인증정보 - 비밀번호, API 키 등"));
    }
}
