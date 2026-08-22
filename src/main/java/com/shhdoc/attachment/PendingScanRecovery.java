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
        List<Attachment> pending = attachmentRepository.findByScanStatus(ScanStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        log.info("검사가 끝나지 않은 첨부 {}건을 다시 요청한다", pending.size());
        pending.forEach(attachment -> bridge.requestScan(new AttachmentRegisteredEvent(attachment.getId())));
    }
}
