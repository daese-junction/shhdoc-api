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
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Parse/Classify/Extract를 병렬 호출해 하나의 {@link DocumentAnalysisResult}로 합칩니다. */
@Component
@RequiredArgsConstructor
public class DocumentAnalyzerImpl implements DocumentAnalyzer {

    private final DocumentParser documentParser;
    private final DocumentClassifier documentClassifier;
    private final InformationExtractor informationExtractor;
    private final DocumentTypeRepository documentTypeRepository;
    private final SensitiveInfoTypeRepository sensitiveInfoTypeRepository;

    @Override
    public DocumentAnalysisResult analyze(DocumentFile file, Long companyId) {
        List<DocumentCategory> categories = categoriesFor(companyId);
        List<SensitiveInfoCategory> sensitiveTypes = sensitiveTypesFor(companyId);

        CompletableFuture<ParsedDocument> parseFuture =
                CompletableFuture.supplyAsync(() -> documentParser.parse(file));
        CompletableFuture<ClassificationResult> classifyFuture =
                CompletableFuture.supplyAsync(() -> documentClassifier.classify(file, categories));
        CompletableFuture<ExtractionResult> extractFuture =
                CompletableFuture.supplyAsync(() -> informationExtractor.extract(file, sensitiveTypes));

        CompletableFuture.allOf(parseFuture, classifyFuture, extractFuture).join();

        return new DocumentAnalysisResult(parseFuture.join(), classifyFuture.join(), extractFuture.join());
    }

    /**
     * 회사가 등록한 문서유형으로 분류 카테고리를 만든다. 아직 하나도 등록 안 된 회사면
     * (신규 회사, seed 전) ShhDoc 기본 taxonomy로 폴백한다 — 카테고리 자체가 없으면
     * 분류 요청 자체를 못 보낸다.
     */
    private List<DocumentCategory> categoriesFor(Long companyId) {
        List<DocumentType> documentTypes = documentTypeRepository.findByCompanyIdOrderByIdAsc(companyId);
        if (documentTypes.isEmpty()) {
            return DefaultDocumentCategories.ALL;
        }
        return documentTypes.stream()
                .map(type -> new DocumentCategory(type.getCode(), type.getName() + " - " + type.getDescription()))
                .toList();
    }

    /** 회사가 민감정보 유형을 하나도 등록 안 했으면 ShhDoc 기본값으로 폴백한다. */
    private List<SensitiveInfoCategory> sensitiveTypesFor(Long companyId) {
        List<SensitiveInfoType> sensitiveInfoTypes = sensitiveInfoTypeRepository.findByCompanyIdOrderByIdAsc(companyId);
        if (sensitiveInfoTypes.isEmpty()) {
            return DefaultSensitiveInfoTypes.ALL;
        }
        return sensitiveInfoTypes.stream()
                .map(type -> new SensitiveInfoCategory(type.getCode(), type.getName() + " - " + type.getDescription()))
                .toList();
    }
}
