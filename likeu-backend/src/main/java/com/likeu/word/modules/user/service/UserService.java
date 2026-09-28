package com.likeu.word.modules.user.service;

import com.likeu.word.modules.user.dto.LoginDTO;
import com.likeu.word.modules.user.vo.LoginVO;
import com.likeu.word.modules.user.vo.UserStatsVO;

/**
 * 用户 Service 接口
 */
public interface UserService {

    /**
     * 登录（首次调用即注册）
     *
     * @param dto      登录参数，{@code code} 仅在拿不到云托管注入的 openid 时才需要
     * @param wxOpenid 云托管注入的 openid（请求头 {@code X-WX-OPENID}），可能为空
     */
    LoginVO login(LoginDTO dto, String wxOpenid);

    /**
     * 按 openid 取得 userId，用户不存在时自动创建
     *
     * <p>供鉴权拦截器使用：云托管链路下每次请求都带 openid，
     * 因此用户可以在首次访问任意接口时就完成注册，无需单独调用登录接口。</p>
     */
    Long resolveUserIdByOpenid(String openid);

    /**
     * 获取用户学习统计
     */
    UserStatsVO getUserStats(Long userId);

    /**
     * 重置用户学习进度
     */
    void resetProgress(Long userId);
}
