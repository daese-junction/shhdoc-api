package com.shhdoc.upstage.context;

import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.dto.MailRequest;

/**
 * 메일 + 문서분석결과를 결합해 판단용 컨텍스트를 조립합니다 (CONTEXT 단계).
 *
 * <p>정책(Policy)은 여기서 쓰지 않는다 — 지금 정책 데이터엔 "수신자 도메인 → 유형"
 * 매핑이 없어서 조립 단계에서 활용할 게 없다. 정책 매칭은 {@code decision.DecisionEngine}이
 * 이 결과를 갖고 별도로 수행한다.
 */
public interface ContextBuilder {

    /**
     * 판단용 컨텍스트를 조립합니다.
     *
     * @param mail      원본 메일 데이터
     * @param docResult 첨부파일 1건에 대한 분석 결과 (판정은 첨부파일 단위라 여러 개면
     *                  호출측이 첨부파일마다 이 메서드를 따로 호출한다)
     * @return 조립된 판단용 컨텍스트
     */
    MailContext build(MailRequest mail, DocumentAnalysisResult docResult);
}
