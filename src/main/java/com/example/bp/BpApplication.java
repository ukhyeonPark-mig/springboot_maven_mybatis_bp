package com.example.bp;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/* 현재는 @Async 메서드가 없어 꺼 둔다.
   메일·OTP는 의도적으로 동기 처리해 contact/OTP 흐름에서 성공/실패를 즉시 보여준다.
   무거운/재시도 작업을 @Async로 빼게 되면 아래 한 줄과 해당 import의 주석을 풀면 된다.
*/
// import org.springframework.scheduling.annotation.EnableAsync;
// @EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class BpApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));  // 애플리케이션 전역 타임존 = Asia/Seoul
        SpringApplication.run(BpApplication.class, args);
    }
}
