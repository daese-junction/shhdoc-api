package com.shhdoc.upstage.document;

import com.shhdoc.upstage.pipeline.DocumentFile;

/** pipeline의 Parse/Classify/Extract를 조합 호출해 하나의 결과로 통합합니다 (UNDERSTAND 단계). */
public interface DocumentAnalyzer {

    /**
     * 회사별 분류/추출 후보 어휘를 로드합니다. 메일 하나에 첨부가 여럿이어도 같은 값이라,
     * 호출측이 메일당 한 번만 호출해서 그 결과를 모든 첨부의 {@link #analyze}에 재사용해야 합니다.
     *
     * @param companyId 대상 회사
     * @return 회사가 등록한 문서유형/민감정보유형. 하나도 등록 안 됐으면 ShhDoc 기본값으로 폴백
     */
    CompanyVocabulary loadVocabulary(Long companyId);

    /**
     * 첨부파일 하나를 분석합니다.
     *
     * @param file       분석 대상 파일 (원본 바이트 포함)
     * @param vocabulary {@link #loadVocabulary}로 메일당 한 번 미리 구해둔 회사별 어휘
     * @return 통합된 분석 결과
     */
    DocumentAnalysisResult analyze(DocumentFile file, CompanyVocabulary vocabulary);
}
