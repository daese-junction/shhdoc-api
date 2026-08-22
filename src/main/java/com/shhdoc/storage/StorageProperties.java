package com.shhdoc.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
public record StorageProperties(
        String endpoint,
        String publicEndpoint,
        String bucket,
        String accessKey,
        String secretKey,
        long presignMinutes) {
}
