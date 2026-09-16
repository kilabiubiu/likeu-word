package com.likeu.word.modules.user.controller;

import com.likeu.word.common.Result;
import com.likeu.word.modules.user.dto.LoginDTO;
import com.likeu.word.modules.user.service.UserService;
import com.likeu.word.modules.user.vo.LoginVO;
import com.likeu.word.modules.user.vo.UserStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 用户 Controller
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 微信小程序登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        log.info("用户登录成功: userId={}", vo.getUserId());
        return Result.success(vo);
    }

    /**
     * 获取用户学习统计
     */
    @GetMapping("/stats")
    public Result<UserStatsVO> stats(@RequestAttribute Long userId) {
        UserStatsVO stats = userService.getUserStats(userId);
        return Result.success(stats);
    }

    /**
     * 重置学习进度
     */
    @PostMapping("/reset")
    public Result<String> reset(@RequestAttribute Long userId) {
        userService.resetProgress(userId);
        return Result.success("重置成功");
    }
}