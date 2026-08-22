package com.shhdoc.upstage.document;

import com.shhdoc.upstage.pipeline.DocumentFile;
import com.shhdoc.upstage.pipeline.classify.ClassificationResult;
import com.shhdoc.upstage.pipeline.classify.DefaultDocumentCategories;
import com.shhdoc.upstage.pipeline.classify.DocumentClassifier;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import com.shhdoc.upstage.pipeline.extract.InformationExtractor;
import com.shhdoc.upstage.pipeline.parse.DocumentParser;
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/** Parse/Classify/Extract를 병렬 호출해 하나의 {@link DocumentAnalysisResult}로 합칩니다. */
@Component
@RequiredArgsConstructor
public class DocumentAnalyzerImpl implements DocumentAnalyzer {

    private final DocumentParser documentParser;
    private final DocumentClassifier documentClassifier;
    private final InformationExtractor informationExtractor;

    @Override
    public DocumentAnalysisResult analyze(DocumentFile file) {
        CompletableFuture<ParsedDocument> parseFuture =
                CompletableFuture.supplyAsync(() -> documentParser.parse(file));
        CompletableFuture<ClassificationResult> classifyFuture =
                CompletableFuture.supplyAsync(() -> documentClassifier.classify(file, DefaultDocumentCategories.ALL));
        CompletableFuture<ExtractionResult> extractFuture =
                CompletableFuture.supplyAsync(() -> informationExtractor.extract(file));

        CompletableFuture.allOf(parseFuture, classifyFuture, extractFuture).join();

        return new DocumentAnalysisResult(parseFuture.join(), classifyFuture.join(), extractFuture.join());
    }
}
