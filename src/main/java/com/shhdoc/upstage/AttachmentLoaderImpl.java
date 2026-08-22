package com.shhdoc.upstage;

import com.shhdoc.upstage.dto.Attachment;
import com.shhdoc.upstage.pipeline.DocumentFile;
import org.springframework.stereotype.Component;

/** TODO: 스토리지 담당자 확인 후 실제 바이트 조회 로직 채우기. */
@Component
public class AttachmentLoaderImpl implements AttachmentLoader {

    @Override
    public DocumentFile load(Attachment attachment) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
