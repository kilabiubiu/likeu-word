package com.likeu.word.modules.user.service;

import com.likeu.word.modules.user.dto.LoginDTO;
import com.likeu.word.modules.user.vo.LoginVO;
import com.likeu.word.modules.user.vo.UserStatsVO;

/**
 * 用户 Service 接口
 */
public interface UserService {

    /**
     * 微信登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取用户学习统计
     */
    UserStatsVO getUserStats(Long userId);

    /**
     * 重置用户学习进度
     */
    void resetProgress(Long userId);
}