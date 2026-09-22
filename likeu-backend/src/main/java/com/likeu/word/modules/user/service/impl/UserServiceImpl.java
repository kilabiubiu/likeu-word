package com.likeu.word.modules.user.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.likeu.word.common.ResultCode;
import com.likeu.word.common.exception.BusinessException;
import com.likeu.word.common.util.JwtUtil;
import com.likeu.word.common.util.RedisUtil;
import com.likeu.word.mapper.UserDailyMapper;
import com.likeu.word.mapper.UserFavRootMapper;
import com.likeu.word.mapper.UserFavWordMapper;
import com.likeu.word.mapper.UserMapper;
import com.likeu.word.mapper.UserWordMapper;
import com.likeu.word.modules.favorite.entity.UserFavRootEntity;
import com.likeu.word.modules.favorite.entity.UserFavWordEntity;
import com.likeu.word.modules.study.entity.UserDailyEntity;
import com.likeu.word.modules.study.entity.UserWordEntity;
import com.likeu.word.modules.user.dto.LoginDTO;
import com.likeu.word.modules.user.entity.UserEntity;
import com.likeu.word.modules.user.service.UserService;
import com.likeu.word.modules.user.vo.LoginVO;
import com.likeu.word.modules.user.vo.UserStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
    private UserWordMapper userWordMapper;

    @Resource
    private UserDailyMapper userDailyMapper;

    @Resource
    private UserFavWordMapper userFavWordMapper;

    @Resource
    private UserFavRootMapper userFavRootMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisUtil redisUtil;

    private static final String WX_CODE_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        // 开发环境模拟登录（当appid是测试值时）
        String openid;
        if ("wx-test-appid".equals(appid)) {
            openid = "dev_openid_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("开发环境模拟登录，生成 openid: {}", maskOpenid(openid));
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
            log.info("新用户注册: id={}, openid={}", user.getId(), maskOpenid(openid));
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
        int totalWords = countUserWords(userId, null);
        int masteredWords = countUserWords(userId, 2);
        int favWords = userFavWordMapper.selectCount(
                new LambdaQueryWrapper<UserFavWordEntity>()
                        .eq(UserFavWordEntity::getUserId, userId)).intValue();
        int favRoots = userFavRootMapper.selectCount(
                new LambdaQueryWrapper<UserFavRootEntity>()
                        .eq(UserFavRootEntity::getUserId, userId)).intValue();
        return new UserStatsVO(totalWords, masteredWords, favWords, favRoots);
    }

    /**
     * 统计用户学习记录数
     *
     * @param status 学习状态，为 null 时统计全部
     */
    private int countUserWords(Long userId, Integer status) {
        LambdaQueryWrapper<UserWordEntity> wrapper = new LambdaQueryWrapper<UserWordEntity>()
                .eq(UserWordEntity::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserWordEntity::getStatus, status);
        }
        return userWordMapper.selectCount(wrapper).intValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetProgress(Long userId) {
        // 删除用户学习记录与每日统计（收藏不清除，用户可自行决定）
        userWordMapper.delete(
                new LambdaQueryWrapper<UserWordEntity>()
                        .eq(UserWordEntity::getUserId, userId));
        userDailyMapper.delete(
                new LambdaQueryWrapper<UserDailyEntity>()
                        .eq(UserDailyEntity::getUserId, userId));
        log.info("用户重置进度完成: userId={}", userId);
    }

    /**
     * 微信 code2Session 换取 openid
     *
     * <p>响应体可能包含 openid、session_key 等敏感字段，因此失败时只记录错误码与描述；
     * 抛给上层的文案固定，不拼接微信原始响应或异常信息。</p>
     */
    private String wxCode2Openid(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WX_CODE_URL, appid, secret, code);

        String resp;
        try {
            resp = HttpUtil.get(url, 5000);
        } catch (Exception e) {
            log.error("调用微信登录接口异常: code={}", code, e);
            throw new BusinessException(ResultCode.WX_LOGIN_FAIL, "微信登录失败，请稍后重试");
        }

        JSONObject json;
        try {
            json = JSONUtil.parseObj(resp);
        } catch (Exception e) {
            log.error("微信登录响应解析失败", e);
            throw new BusinessException(ResultCode.WX_LOGIN_FAIL, "微信登录失败，请稍后重试");
        }

        String openid = json.getStr("openid");
        if (openid == null) {
            log.error("微信登录失败: errcode={}, errmsg={}", json.getInt("errcode"), json.getStr("errmsg"));
            throw new BusinessException(ResultCode.WX_LOGIN_FAIL, "微信登录失败，请稍后重试");
        }
        return openid;
    }

    /**
     * 掩码处理用户标识，只保留尾 4 位，避免 openid 明文进入日志
     */
    private static String maskOpenid(String openid) {
        if (openid == null || openid.length() <= 4) {
            return "****";
        }
        return "****" + openid.substring(openid.length() - 4);
    }
}
