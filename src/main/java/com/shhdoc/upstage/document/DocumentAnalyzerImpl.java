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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Parse/Classify/Extract를 병렬 호출해 하나의 {@link DocumentAnalysisResult}로 합칩니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAnalyzerImpl implements DocumentAnalyzer {

    private final DocumentParser documentParser;
    private final DocumentClassifier documentClassifier;
    private final InformationExtractor informationExtractor;
    private final DocumentTypeRepository documentTypeRepository;
    private final SensitiveInfoTypeRepository sensitiveInfoTypeRepository;

    @Override
    public CompanyVocabulary loadVocabulary(Long companyId) {
        return new CompanyVocabulary(categoriesFor(companyId), sensitiveTypesFor(companyId));
    }

    @Override
    public DocumentAnalysisResult analyze(DocumentFile file, CompanyVocabulary vocabulary) {
        log.info("[UNDERSTAND] file={} 시작 (Parse/Classify/Extract 병렬 호출)", file.fileName());
        long startedAt = System.currentTimeMillis();

        // supplyAsync는 공용 ForkJoinPool 스레드에서 도는데 MDC는 스레드로컬이라
        // 이 스레드(mailId 포함)의 MDC가 자동으로 안 넘어간다 — 넘겨주지 않으면
        // PARSE/CLASSIFY/EXTRACT 로그에 mailId가 안 찍힌다.
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        CompletableFuture<ParsedDocument> parseFuture =
                CompletableFuture.supplyAsync(withMdc(mdcContext, () -> documentParser.parse(file)));
        CompletableFuture<ClassificationResult> classifyFuture =
                CompletableFuture.supplyAsync(withMdc(mdcContext, () -> documentClassifier.classify(file, vocabulary.categories())));
        CompletableFuture<ExtractionResult> extractFuture =
                CompletableFuture.supplyAsync(withMdc(mdcContext, () -> informationExtractor.extract(file, vocabulary.sensitiveTypes())));

        CompletableFuture.allOf(parseFuture, classifyFuture, extractFuture).join();

        DocumentAnalysisResult result =
                new DocumentAnalysisResult(parseFuture.join(), classifyFuture.join(), extractFuture.join());
        log.info("[UNDERSTAND] file={} 완료 {}ms", file.fileName(), System.currentTimeMillis() - startedAt);
        return result;
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

    /**
     * 호출측 스레드의 MDC(mailId 등)를 supplyAsync가 실행되는 공용 풀 스레드에 복원한다.
     * 그 스레드가 이전에 다른 요청 처리로 MDC를 남겨뒀을 수도 있어서, 끝나면 원래 값으로
     * 되돌리거나(이 스레드가 그 전에 갖고 있던 컨텍스트) 없으면 지운다 — 공용 풀이라
     * 남겨두면 다음 무관한 작업에 새어나간다.
     */
    private static <T> Supplier<T> withMdc(Map<String, String> mdcContext, Supplier<T> supplier) {
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            try {
                return supplier.get();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
