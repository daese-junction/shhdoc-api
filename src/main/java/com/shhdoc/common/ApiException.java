package com.shhdoc.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 이 프로젝트의 유일한 비즈니스 예외. ErrorCode enum 없이 상태코드 + 메시지만 담는다. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
