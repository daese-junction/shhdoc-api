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
        CompanyVocabulary vocabulary = new CompanyVocabulary(DefaultDocumentCategories.ALL, DefaultSensitiveInfoTypes.ALL);

        when(documentParser.parse(file)).thenReturn(parsed);
        when(documentClassifier.classify(file, vocabulary.categories())).thenReturn(classification);
        when(informationExtractor.extract(file, vocabulary.sensitiveTypes())).thenReturn(extraction);

        DocumentAnalysisResult result = documentAnalyzer.analyze(file, vocabulary);

        assertThat(result.parsed()).isSameAs(parsed);
        assertThat(result.classification()).isSameAs(classification);
        assertThat(result.extraction()).isSameAs(extraction);
    }

    @Test
    void 회사에_등록된_문서유형이_없으면_기본taxonomy로_로드한다() {
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());

        CompanyVocabulary vocabulary = documentAnalyzer.loadVocabulary(COMPANY_ID);

        assertThat(vocabulary.categories()).isEqualTo(DefaultDocumentCategories.ALL);
    }

    @Test
    void 회사에_등록된_문서유형이_있으면_그걸로_로드한다() {
        DocumentType documentType = Mockito.mock(DocumentType.class);
        when(documentType.getCode()).thenReturn("PAYROLL");
        when(documentType.getName()).thenReturn("급여명세서");
        when(documentType.getDescription()).thenReturn("급여 지급/공제 내역 문서");
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of(documentType));
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());

        CompanyVocabulary vocabulary = documentAnalyzer.loadVocabulary(COMPANY_ID);

        assertThat(vocabulary.categories())
                .containsExactly(new DocumentCategory("PAYROLL", "급여명세서 - 급여 지급/공제 내역 문서"));
    }

    @Test
    void 회사에_등록된_민감정보유형이_없으면_기본값으로_로드한다() {
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());

        CompanyVocabulary vocabulary = documentAnalyzer.loadVocabulary(COMPANY_ID);

        assertThat(vocabulary.sensitiveTypes()).isEqualTo(DefaultSensitiveInfoTypes.ALL);
    }

    @Test
    void 회사에_등록된_민감정보유형이_있으면_그걸로_로드한다() {
        SensitiveInfoType sensitiveInfoType = Mockito.mock(SensitiveInfoType.class);
        when(sensitiveInfoType.getCode()).thenReturn("CREDENTIAL");
        when(sensitiveInfoType.getName()).thenReturn("인증정보");
        when(sensitiveInfoType.getDescription()).thenReturn("비밀번호, API 키 등");
        when(documentTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of());
        when(sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(COMPANY_ID)).thenReturn(List.of(sensitiveInfoType));

        CompanyVocabulary vocabulary = documentAnalyzer.loadVocabulary(COMPANY_ID);

        assertThat(vocabulary.sensitiveTypes())
                .containsExactly(new SensitiveInfoCategory("CREDENTIAL", "인증정보 - 비밀번호, API 키 등"));
    }

    @Test
    void analyze는_어휘를_다시_조회하지_않고_주어진_vocabulary를_그대로_쓴다() {
        DocumentFile file = new DocumentFile("test.pdf", "dummy".getBytes());
        CompanyVocabulary vocabulary = new CompanyVocabulary(DefaultDocumentCategories.ALL, DefaultSensitiveInfoTypes.ALL);
        when(documentParser.parse(file)).thenReturn(new ParsedDocument(new ParsedContent("", "", ""), List.of(), 1));
        ArgumentCaptor<List<DocumentCategory>> categoryCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<SensitiveInfoCategory>> sensitiveCaptor = ArgumentCaptor.forClass(List.class);
        when(documentClassifier.classify(any(), categoryCaptor.capture())).thenReturn(new ClassificationResult("general", 1.0));
        when(informationExtractor.extract(any(), sensitiveCaptor.capture())).thenReturn(new ExtractionResult(List.of(), null, ""));

        documentAnalyzer.analyze(file, vocabulary);

        assertThat(categoryCaptor.getValue()).isEqualTo(DefaultDocumentCategories.ALL);
        assertThat(sensitiveCaptor.getValue()).isEqualTo(DefaultSensitiveInfoTypes.ALL);
        verify(documentTypeRepository, Mockito.never()).findByCompanyIdOrderByIdAsc(any());
        verify(sensitiveInfoTypeRepository, Mockito.never()).findByCompanyIdOrderByIdAsc(any());
    }
}
