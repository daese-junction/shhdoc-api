package com.shhdoc.upstage;

import com.shhdoc.upstage.dto.Attachment;
import com.shhdoc.upstage.pipeline.DocumentFile;

/**
 * {@code Attachment}(storageKey만 보유)를 Upstage API로 보낼 수 있는 실제 바이트({@code DocumentFile})로
 * 변환합니다. upstage는 스토리지에 직접 접근하지 않으므로, 이 구현은 스토리지를 가진
 * 메일 제공사 모듈 쪽 컴포넌트를 호출해야 합니다.
 *
 * <p>스토리지(MinIO/S3)는 지금 "파일 바이트가 앱을 거치지 않는다"는 설계라 바이트를
 * 읽어오는 방법이 아직 없음 — 담당자 확인 후 실제 구현을 채운다.
 */
public interface AttachmentLoader {

    /**
     * 첨부파일의 실제 바이트를 읽어옵니다.
     *
     * @param attachment storageKey를 포함한 첨부파일 메타데이터
     * @return Upstage API로 전송 가능한 파일
     */
    DocumentFile load(Attachment attachment);
}
