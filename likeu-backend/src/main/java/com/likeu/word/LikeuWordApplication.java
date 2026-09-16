package com.likeu.word;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.likeu.word.mapper")
public class LikeuWordApplication {

    public static void main(String[] args) {
        SpringApplication.run(LikeuWordApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  LikeU 词根背单词后端启动成功 ✅");
        System.out.println("  访问地址: http://localhost:8080/api");
        System.out.println("  H2 控制台: http://localhost:8080/api/h2-console");
        System.out.println("========================================\n");
    }
}