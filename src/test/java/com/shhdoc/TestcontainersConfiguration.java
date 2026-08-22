package com.shhdoc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * 통합 테스트용 MySQL. 테스트 클래스에 @Import(TestcontainersConfiguration.class) 만 붙이면
 * 컨테이너가 뜨고 datasource 설정까지 자동으로 주입됩니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.4");
    }
}
