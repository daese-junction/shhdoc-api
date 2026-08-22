package com.shhdoc.upstage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * {@link MailProcessor}용 워커 스레드풀. 메일끼리는 서로 순서를 지킬 이유가 없어서
 * 여러 개를 동시에 처리한다 — 동시처리 개수만 이 풀 사이즈로 제한한다.
 * 같은 mailId가 중복 처리되는 건 {@link com.shhdoc.upstage.mail.Mail#tryMarkProcessing()}이 막고,
 * Upstage API의 낮은 RPS(특히 Document Parse 동기 RPS=1)는 각 pipeline 구현체 쪽에서
 * 별도로 제한한다 — 이 풀 사이즈로 API rate limit을 대신하지 않는다.
 */
@Configuration
@EnableAsync
public class MailProcessorConfig {

    public static final String EXECUTOR_BEAN_NAME = "mailProcessorExecutor";

    @Bean(EXECUTOR_BEAN_NAME)
    public Executor mailProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("mail-processor-");
        executor.initialize();
        return executor;
    }
}
