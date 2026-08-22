package com.shhdoc.attachment;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 검사 큐가 인메모리라 앱이 죽으면 진행 중이던 요청이 사라진다. 그 첨부는 PENDING 인 채로
 * 남아 화면은 "검사 중"을 무한히 돌고 발송도 막힌다. 기동할 때 한 번 다시 요청해 되살린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingScanRecovery implements ApplicationRunner {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentScanBridge bridge;

    @Override
    public void run(ApplicationArguments args) {
        // PENDING 만 되살린다. FAILED 는 일부러 뺐다 — 외부 API 가 죽어 있는 동안 실패가 쌓이면
        // 재시작할 때마다 그 전량이 한꺼번에 다시 나가고, 손상된 파일처럼 영구히 실패하는 건도
        // 매번 재시도된다. 실패한 첨부는 재검사(POST /attachments/{id}/rescan)로 사람이 되돌린다.
        List<Attachment> pending = attachmentRepository.findByScanStatus(ScanStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        log.info("검사가 끝나지 않은 첨부 {}건을 다시 요청한다", pending.size());
        for (Attachment attachment : pending) {
            // 한 건이 터져도 기동은 계속돼야 한다. 여기서 예외가 나가면 앱이 아예 뜨지 않는다.
            try {
                bridge.requestScan(new AttachmentRegisteredEvent(attachment.getId()));
            } catch (RuntimeException e) {
                log.error("첨부 {} 재요청 실패 — 건너뛴다", attachment.getId(), e);
            }
        }
    }
}
