package com.likeu.word;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
@MapperScan("com.likeu.word.mapper")
public class LikeuWordApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(LikeuWordApplication.class, args);
        Environment env = context.getEnvironment();
        String[] profiles = env.getActiveProfiles();
        log.info("LikeU 词根背单词后端启动成功, 地址=http://localhost:{}{}, profiles={}",
                env.getProperty("server.port"),
                env.getProperty("server.servlet.context-path", ""),
                profiles.length == 0 ? "default" : String.join(",", profiles));
    }
}
