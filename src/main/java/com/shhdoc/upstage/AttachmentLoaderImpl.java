package com.shhdoc.upstage;

import com.shhdoc.storage.StorageProperties;
import com.shhdoc.upstage.dto.Attachment;
import com.shhdoc.upstage.pipeline.DocumentFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Component
@RequiredArgsConstructor
public class AttachmentLoaderImpl implements AttachmentLoader {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public DocumentFile load(Attachment attachment) {
        String storageKey = attachment.storageKey();
        try (InputStream in = s3Client.getObject(GetObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(storageKey)
                .build())) {
            return new DocumentFile(attachment.fileName(), in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read attachment: " + storageKey, e);
        }
    }
}
