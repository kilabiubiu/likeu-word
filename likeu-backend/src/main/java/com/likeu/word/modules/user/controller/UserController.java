package com.likeu.word.modules.user.controller;

import com.likeu.word.common.Result;
import com.likeu.word.common.interceptor.AuthInterceptor;
import com.likeu.word.modules.user.dto.LoginDTO;
import com.likeu.word.modules.user.service.UserService;
import com.likeu.word.modules.user.vo.LoginVO;
import com.likeu.word.modules.user.vo.UserStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户 Controller
 */
@Tag(name = "用户", description = "登录、学习统计与进度重置")
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 登录（首次调用即注册）
     *
     * <p>免登录接口。经云托管 callContainer 调用时，请求头带有微信网关注入的 openid，
     * 直接用它完成注册/登录；本地开发走 wx.login 的 code + code2Session。</p>
     */
    @Operation(summary = "登录/注册", description = "免登录接口，返回 token 与 userId；callContainer 链路无需传 code")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody(required = false) LoginDTO dto,
                                 @RequestHeader(value = AuthInterceptor.WX_OPENID_HEADER, required = false)
                                 String wxOpenid) {
        LoginVO vo = userService.login(dto, wxOpenid);
        log.info("用户登录成功: userId={}", vo.getUserId());
        return Result.success(vo);
    }

    /**
     * 获取用户学习统计
     */
    @Operation(summary = "获取用户学习统计")
    @GetMapping("/stats")
    public Result<UserStatsVO> stats(@RequestAttribute Long userId) {
        UserStatsVO stats = userService.getUserStats(userId);
        return Result.success(stats);
    }

    /**
     * 重置学习进度
     */
    @Operation(summary = "重置学习进度")
    @PostMapping("/reset")
    public Result<String> reset(@RequestAttribute Long userId) {
        userService.resetProgress(userId);
        return Result.success("重置成功");
    }
}
