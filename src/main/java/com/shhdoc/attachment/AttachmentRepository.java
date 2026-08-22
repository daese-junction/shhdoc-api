package com.shhdoc.attachment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEmailIdOrderByIdAsc(Long emailId);

    /** 같은 파일이 이미 검사됐는지 확인해 판정을 재사용한다. */
    Optional<Attachment> findFirstByContentHashAndScanStatusOrderByIdAsc(String contentHash, ScanStatus scanStatus);

    /** 발송 판정용. 첨부 담당 모듈이 결과를 채우면 이 질문 하나로 보류 여부가 정해진다. */
    boolean existsByEmailIdAndVerdict(Long emailId, Verdict verdict);

    /** 검사가 끝나지 않은 첨부가 남았는지. 판정 전에 나가는 것을 막는다. */
    boolean existsByEmailIdAndScanStatus(Long emailId, ScanStatus scanStatus);

    /** 기동 시 되살릴 대상. 인메모리 큐가 날아가도 이 행들은 DB 에 남아 있다. */
    List<Attachment> findByScanStatus(ScanStatus scanStatus);

    /** 판정 결과는 storageKey 로 돌아온다. 컬럼에 unique 가 걸려 있어 한 건만 나온다. */
    Optional<Attachment> findByStorageKey(String storageKey);
}
