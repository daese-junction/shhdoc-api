package com.shhdoc.upstage;

import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.MailStatusResponse;

import java.util.List;

/**
 * 메일 서비스 제공사 모듈(이 패키지 외부, 즉 upstage가 아닌 나머지 모놀리식 코드)이
 * ShhDoc 분석 모듈({@code com.shhdoc.upstage} 하위)을 호출하기 위한 유일한 진입점입니다.
 * upstage 하위 패키지의 다른 클래스를 직접 호출하지 말고 이 인터페이스를 통해서만 연동하세요.
 *
 * <p>호출 흐름:
 * <ol>
 *   <li>메일 제공사 모듈이 {@link #enqueue}로 첨부파일 있는 메일을 등록 (비동기 처리 시작)</li>
 *   <li>진행 상황이 궁금하면 {@link #getStatus}로 폴링 ?? 조회??</li>
 *   <li>처리가 끝나면 ShhDoc이 {@link #publishDecision}으로 결과를 메일 제공사 모듈에 통보</li>
 * </ol>
 */
public interface Gateway {

    /**
     * 메일 제공사 모듈에서 HOLD된 메일을 ShhDoc 처리 큐에 등록합니다.
     * 등록만 하고 즉시 리턴되며, 실제 문서분석/정책판단은 비동기로 진행됩니다.
     * 처리 결과는 이 메서드의 리턴값이 아니라 {@link #getStatus} 폴링 또는
     * {@link #publishDecision} 통보로 확인해야 합니다.
     *
     * @param request 분석 대상 메일 데이터. {@code mailId}는 호출측(메일 제공사)에서
     *                발급한 식별자를 그대로 전달하며, 이후 {@link #getStatus}/
     *                {@link #publishDecision}에서 동일 식별자로 매칭됩니다.
     */
    void enqueue(MailRequest request);

    /**
     * 특정 기업의 메일 중 아직 결과가 발행되지 않은(PENDING/PROCESSING) 건들의
     * 처리 상태를 조회합니다. 처리가 완료(DONE)되어 {@link #publishDecision}으로
     * 이미 통보된 메일은 이 목록에 포함되지 않습니다.
     *
     * @param companyId 조회 대상 기업 식별자
     * @return 미완료 메일들의 처리 상태 목록. 미완료 건이 없으면 빈 리스트를 반환합니다.
     */
    List<MailStatusResponse> getStatus(Integer companyId);

    /**
     * ShhDoc이 처리를 완료한 메일의 판단 결과(첨부파일별 ALLOW/REVIEW)를
     * 메일 제공사 모듈에 발행(통보)합니다. ShhDoc 내부 판단 로직이 호출하는
     * 아웃바운드 콜백이며, 메일 제공사 모듈은 이 결과를 받아 실제 발송/보류/차단을 수행합니다.
     *
     * @param response 판단이 완료된 메일의 결과. {@code mailId}는 {@link #enqueue}에서
     *                 전달받은 식별자와 동일합니다.
     */
    void publishDecision(DecisionResponse response);
}
