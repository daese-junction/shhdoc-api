package com.shhdoc.storage;

import com.shhdoc.common.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** 파일 바이트는 앱을 거치지 않는다. 서버는 키 발급과 서명 URL 만 담당한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    private volatile boolean bucketReady = false;

    /** 프론트가 이 URL 로 파일을 직접 PUT 한다. */
    public PresignedUpload presignUpload(String filename) {
        ensureBucket();
        // 사용자가 준 파일명을 키로 쓰지 않는다. 경로 조작과 중복 덮어쓰기를 한 번에 막는다.
        String key = UUID.randomUUID().toString();
        URL url = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(presignDuration())
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(properties.bucket())
                                .key(key)
                                .build())
                        .build())
                .url();
        log.debug("presigned upload for {} -> {}", filename, key);
        return new PresignedUpload(key, url.toString(), presignDuration().toSeconds());
    }

    /** 프론트가 이 URL 로 파일을 직접 연다. */
    public PresignedDownload presignDownload(String key, String filename) {
        URL url = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(presignDuration())
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(properties.bucket())
                                .key(key)
                                // 원본 파일명은 여기서 되살린다. 저장 키는 UUID 라서.
                                .responseContentDisposition(contentDisposition(filename))
                                .build())
                        .build())
                .url();
        return new PresignedDownload(url.toString(), presignDuration().toSeconds());
    }

    /** 프론트가 실제로 올렸는지 확인한다. 안 올리고 등록만 하는 것을 막는다. */
    public long requireUploaded(String key) {
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
            return head.contentLength();
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "업로드되지 않은 파일입니다. 먼저 업로드를 마쳐주세요.");
        }
    }

    /**
     * 저장된 객체를 읽어 SHA-256 을 계산한다. 파일이 앱을 거치지 않으므로
     * 업로드가 끝난 뒤에 한 번 읽는다. 같은 파일이면 이전 판정을 재사용하는 데 쓴다.
     */
    public String sha256(String key) {
        try (InputStream in = s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "첨부파일을 읽지 못했습니다.");
        }
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build());
    }

    /**
     * 버킷이 없으면 만든다. 시작할 때가 아니라 처음 쓸 때 확인한다.
     * 기동 시점에 확인하면 MinIO 없는 환경(CI 테스트)에서 앱이 아예 안 뜬다.
     */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            if (!bucketExists()) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
                log.info("created bucket {}", properties.bucket());
            }
            bucketReady = true;
        }
    }

    private boolean bucketExists() {
        try {
            s3Client.headBucket(builder -> builder.bucket(properties.bucket()));
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    private Duration presignDuration() {
        return Duration.ofMinutes(properties.presignMinutes());
    }

    /** 파일명에 따옴표나 줄바꿈이 들어가면 헤더가 깨지므로 정리해서 넣는다. */
    private static String contentDisposition(String filename) {
        String safe = Optional.ofNullable(filename).orElse("attachment").replaceAll("[\"\\r\\n]", "_");
        return "attachment; filename=\"" + safe + "\"";
    }

    public record PresignedUpload(String storageKey, String uploadUrl, long expiresInSeconds) {
    }

    public record PresignedDownload(String downloadUrl, long expiresInSeconds) {
    }
}
