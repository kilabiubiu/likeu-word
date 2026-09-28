package com.likeu.word.modules.user.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.likeu.word.common.ResultCode;
import com.likeu.word.common.exception.BusinessException;
import com.likeu.word.common.util.JwtUtil;
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
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 用户 Service 实现
 *
 * <p>身份来源有两类，优先使用云托管注入的 openid：</p>
 * <ul>
 *   <li>小程序经 {@code wx.cloud.callContainer} 调用 → 请求头携带 X-WX-OPENID，直接使用；</li>
 *   <li>本地开发 / 普通 HTTP 客户端 → 用 wx.login 拿到的 code 走 code2Session 换取，
 *       dev 环境（appid=wx-test-appid）直接生成模拟 openid，不调微信接口。</li>
 * </ul>
 *
 * <p>登录态不再依赖 Redis：token 为无状态 JWT，服务端不保存；callContainer 链路下
 * 其实每次请求都能拿到 openid，token 只是为本地调试与普通客户端保留的兼容通道。</p>
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Value("${wx.mini.appid}")
    private String appid;

    @Value("${wx.mini.secret}")
    private String secret;

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

    private static final String WX_CODE_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /** 开发模拟登录使用的 appid：命中时不调用微信接口 */
    private static final String MOCK_APPID = "wx-test-appid";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto, String wxOpenid) {
        // callContainer 链路的小程序可以只发一个空 body，这里统一兜底
        LoginDTO req = dto == null ? new LoginDTO() : dto;
        String openid = resolveOpenid(req, wxOpenid);

        // 查找或创建用户，并同步最新的昵称/头像
        UserEntity user = findOrCreateUser(openid, req.getNickname(), req.getAvatar(), true);

        // 无状态 JWT：只做签名与过期校验，服务端不保存，因此无需 Redis
        String token = jwtUtil.createToken(user.getId());
        return new LoginVO(token, user.getId(), user.getNickname(), user.getAvatar());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resolveUserIdByOpenid(String openid) {
        if (!StringUtils.hasText(openid)) {
            return null;
        }
        UserEntity user = findOrCreateUser(openid.trim(), null, null, false);
        return user.getId();
    }

    /**
     * 确定本次请求的 openid：云托管注入的优先，其次 code2Session，dev 环境走模拟
     */
    private String resolveOpenid(LoginDTO dto, String wxOpenid) {
        if (StringUtils.hasText(wxOpenid)) {
            return wxOpenid.trim();
        }

        String code = dto == null ? null : dto.getCode();
        if (!StringUtils.hasText(code)) {
            // 正常链路不会走到这里：callContainer 一定带 openid；本地调试一定带 code
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少登录凭证：请携带 code 或经小程序云托管调用");
        }

        if (MOCK_APPID.equals(appid)) {
            String mockOpenid = "dev_openid_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("开发环境模拟登录，生成 openid: {}", maskOpenid(mockOpenid));
            return mockOpenid;
        }
        return wxCode2Openid(code);
    }

    /**
     * 按 openid 查找用户，不存在则创建
     *
     * @param syncProfile 是否用传入的昵称/头像更新已有用户（登录时更新，鉴权解析时不做写入）
     */
    private UserEntity findOrCreateUser(String openid, String nickname, String avatar, boolean syncProfile) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOpenid, openid));

        if (user == null) {
            user = new UserEntity();
            user.setOpenid(openid);
            user.setNickname(StringUtils.hasText(nickname) ? nickname : "微信用户");
            user.setAvatar(avatar != null ? avatar : "");
            user.setWordBookId(1L); // 默认绑定第一本词书
            user.setDailyNew(20);
            user.setDailyReview(50);
            userMapper.insert(user);
            log.info("新用户注册: id={}, openid={}", user.getId(), maskOpenid(openid));
            return user;
        }

        if (!syncProfile) {
            return user;
        }

        boolean changed = false;
        if (StringUtils.hasText(nickname) && !nickname.equals(user.getNickname())) {
            user.setNickname(nickname);
            changed = true;
        }
        if (StringUtils.hasText(avatar) && !avatar.equals(user.getAvatar())) {
            user.setAvatar(avatar);
            changed = true;
        }
        if (changed) {
            userMapper.updateById(user);
        }
        return user;
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
     *
     * <p>仅在非 callContainer 链路（本地开发、普通 HTTP 客户端）使用，
     * 需要容器具备公网出网能力；云托管注入 openid 的链路完全不依赖该接口。</p>
     */
    private String wxCode2Openid(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WX_CODE_URL, appid, secret, code);

        String resp;
        try {
            resp = HttpUtil.get(url, 5000);
        } catch (Exception e) {
            log.error("调用微信登录接口异常", e);
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
