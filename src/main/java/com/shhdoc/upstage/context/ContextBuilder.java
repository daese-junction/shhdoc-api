package com.shhdoc.upstage.context;

import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.dto.MailRequest;

/**
 * 메일 + 문서분석결과를 결합해 판단용 컨텍스트를 조립합니다 (CONTEXT 단계).
 *
 * <p>정책(Policy)은 여기서 쓰지 않는다 — 정책 매칭은 {@code decision.DecisionEngine}이
 * 이 결과를 갖고 별도로 수행한다.
 *
 * <p>{@code recipientType}은 메일 하나에 딸린 모든 첨부파일이 공유하는 값(수신자 목록은
 * 첨부파일과 무관)이라, 첨부파일마다 반복 조회하지 않도록 {@link #resolveRecipientType}으로
 * 분리해뒀다 — 호출측이 메일당 한 번만 호출해서 그 결과를 {@link #build}에 넘겨써야 한다.
 */
public interface ContextBuilder {

    /**
     * 메일의 수신자 유형(internal/partner/personal_email/external)을 판정합니다.
     * 실 DB(회사 도메인, 등록된 수신자 도메인)를 조회하므로 메일당 한 번만 호출해야 합니다.
     *
     * @param mail 원본 메일 데이터
     * @return 수신자 유형. 수신자가 없으면 {@code null}
     */
    String resolveRecipientType(MailRequest mail);

    /**
     * 판단용 컨텍스트를 조립합니다.
     *
     * @param mail          원본 메일 데이터
     * @param docResult     첨부파일 1건에 대한 분석 결과 (판정은 첨부파일 단위라 여러 개면
     *                      호출측이 첨부파일마다 이 메서드를 따로 호출한다)
     * @param recipientType {@link #resolveRecipientType}으로 메일당 한 번 미리 구해둔 값
     * @return 조립된 판단용 컨텍스트
     */
    MailContext build(MailRequest mail, DocumentAnalysisResult docResult, String recipientType);
}
