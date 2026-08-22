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
}
