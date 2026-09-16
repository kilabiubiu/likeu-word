package com.likeu.word.modules.user.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.likeu.word.common.exception.BusinessException;
import com.likeu.word.common.util.JwtUtil;
import com.likeu.word.common.util.RedisUtil;
import com.likeu.word.mapper.UserMapper;
import com.likeu.word.modules.user.dto.LoginDTO;
import com.likeu.word.modules.user.entity.UserEntity;
import com.likeu.word.modules.user.service.UserService;
import com.likeu.word.modules.user.vo.LoginVO;
import com.likeu.word.modules.user.vo.UserStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户 Service 实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Value("${wx.mini.appid}")
    private String appid;

    @Value("${wx.mini.secret}")
    private String secret;

    @Value("${jwt.redis-prefix}")
    private String redisPrefix;

    @Value("${jwt.expire-seconds}")
    private Long expireSeconds;

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisUtil redisUtil;

    private final JdbcTemplate jdbcTemplate;

    private static final String WX_CODE_URL = "https://api.weixin.qq.com/sns/jscode2session";

    public UserServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        // 开发环境模拟登录（当appid是测试值时）
        String openid;
        if ("wx-test-appid".equals(appid)) {
            openid = "dev_openid_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("开发环境模拟登录，生成 openid: {}", openid);
        } else {
            // 1. 微信code换取openid
            openid = wxCode2Openid(dto.getCode());
        }

        // 2. 查找或创建用户
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOpenid, openid));
        if (user == null) {
            user = new UserEntity();
            user.setOpenid(openid);
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : "测试用户");
            user.setAvatar(dto.getAvatar() != null ? dto.getAvatar() : "");
            user.setWordBookId(1L); // 默认绑定第一本词书
            user.setDailyNew(20);
            user.setDailyReview(50);
            userMapper.insert(user);
            log.info("新用户注册: id={}, openid={}", user.getId(), openid);
        } else {
            // 更新用户信息
            if (dto.getNickname() != null) {
                user.setNickname(dto.getNickname());
            }
            if (dto.getAvatar() != null) {
                user.setAvatar(dto.getAvatar());
            }
            userMapper.updateById(user);
        }

        // 3. 生成JWT token，存入Redis
        String token = jwtUtil.createToken(user.getId());
        String redisKey = redisPrefix + user.getId();
        redisUtil.set(redisKey, token, expireSeconds, TimeUnit.SECONDS);

        return new LoginVO(token, user.getId(), user.getNickname(), user.getAvatar());
    }

    @Override
    public UserStatsVO getUserStats(Long userId) {
        // 直接使用JDBC查询，避免跨模块编译问题，后续完善
        Integer totalWords = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_user_word WHERE user_id = ?",
                Integer.class, userId);
        Integer masteredWords = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_user_word WHERE user_id = ? AND status = 2",
                Integer.class, userId);
        Integer favWords = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_user_fav_word WHERE user_id = ?",
                Integer.class, userId);
        Integer favRoots = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_user_fav_root WHERE user_id = ?",
                Integer.class, userId);
        return new UserStatsVO(
                totalWords != null ? totalWords : 0,
                masteredWords != null ? masteredWords : 0,
                favWords != null ? favWords : 0,
                favRoots != null ? favRoots : 0
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetProgress(Long userId) {
        // 删除用户学习记录
        jdbcTemplate.update("DELETE FROM t_user_word WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM t_user_daily WHERE user_id = ?", userId);
        // 删除收藏不清除，用户可自行决定
        log.info("用户重置进度完成: userId={}", userId);
    }

    /**
     * 微信 code2Session 换取 openid
     */
    private String wxCode2Openid(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WX_CODE_URL, appid, secret, code);
        try {
            String resp = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(resp);
            String openid = json.getStr("openid");
            if (openid == null) {
                String errMsg = json.getStr("errmsg", "微信登录失败");
                log.error("微信登录失败: {}", resp);
                throw new BusinessException("微信登录失败: " + errMsg);
            }
            return openid;
        } catch (Exception e) {
            log.error("微信登录异常", e);
            throw new BusinessException("微信登录失败: " + e.getMessage());
        }
    }
}