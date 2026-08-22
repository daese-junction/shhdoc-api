package com.shhdoc.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfig {

    /** MinIO 는 버킷을 서브도메인이 아니라 경로로 받는다. path style 을 켜지 않으면 붙지 않는다. */
    private static final S3Configuration PATH_STYLE =
            S3Configuration.builder().pathStyleAccessEnabled(true).build();

    /** MinIO 는 리전을 쓰지 않지만 SDK 가 값을 요구한다. */
    private static final Region REGION = Region.US_EAST_1;

    private static StaticCredentialsProvider credentials(StorageProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }

    /** 서버 ↔ 스토리지 통신용. 내부 주소를 쓴다. */
    @Bean
    S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(PATH_STYLE)
                .region(REGION)
                .build();
    }

    /** 브라우저가 직접 열 URL 을 만든다. 반드시 외부 주소로 서명해야 한다. */
    @Bean
    S3Presigner s3Presigner(StorageProperties properties) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.publicEndpoint()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(PATH_STYLE)
                .region(REGION)
                .build();
    }
}
