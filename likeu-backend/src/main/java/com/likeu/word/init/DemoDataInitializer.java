package com.likeu.word.init;

import com.likeu.word.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * 演示数据初始化器
 * 启动时自动检查并初始化演示数据
 */
@Slf4j
@Component
@Order(1)
@Profile("!dev")
public class DemoDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("classpath:data/init_demo_data.sql")
    private Resource initSqlResource;

    public DemoDataInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        try {
            // 检查是否已有数据
            Integer bookCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM t_word_book WHERE deleted = 0", Integer.class);
            if (bookCount != null && bookCount > 0) {
                log.info("演示数据已存在，跳过初始化 (t_word_book已有 {} 条记录)", bookCount);
                return;
            }

            log.info("===== 开始初始化演示数据 =====");
            String sql = loadSql();
            executeBatch(sql);
            log.info("===== 演示数据初始化完成 ✅ =====");
        } catch (Exception e) {
            log.error("演示数据初始化失败", e);
            throw new BusinessException("演示数据初始化失败: " + e.getMessage());
        }
    }

    private String loadSql() {
        try (InputStream is = initSqlResource.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new BusinessException("读取初始化SQL文件失败: " + e.getMessage());
        }
    }

    private void executeBatch(String sql) {
        // 按分号分割SQL语句，逐条执行
        String[] statements = sql.split(";");
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            int count = 0;
            for (String s : statements) {
                String trimmed = s.trim();
                if (trimmed.length() > 5) {
                    stmt.addBatch(trimmed);
                    count++;
                }
            }
            stmt.executeBatch();
            conn.commit();
            log.info("批量执行SQL完成，共 {} 条语句", count);
        } catch (Exception e) {
            throw new BusinessException("执行初始化SQL失败: " + e.getMessage());
        }
    }
}