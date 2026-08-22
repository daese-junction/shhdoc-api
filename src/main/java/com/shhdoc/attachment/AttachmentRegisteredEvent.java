package com.shhdoc.attachment;

/**
 * 첨부가 DB 에 등록됐을 때 발행된다. 검사 요청은 이 이벤트를 커밋 이후에 받아서 보낸다.
 * 트랜잭션 안에서 바로 보내면 분석 워커가 아직 커밋되지 않은 행을 찾다가 결과를 버린다.
 */
record AttachmentRegisteredEvent(Long attachmentId) {
}
