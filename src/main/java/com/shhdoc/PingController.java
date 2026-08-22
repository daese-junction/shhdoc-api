package com.shhdoc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 배포 헬스체크용. DB까지 확인하려면 actuator를 추가할 것. */
@RestController
public class PingController {

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}
