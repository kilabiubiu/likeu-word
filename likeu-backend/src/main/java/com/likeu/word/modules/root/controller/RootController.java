package com.likeu.word.modules.root.controller;

import com.likeu.word.common.PageVO;
import com.likeu.word.common.Result;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.root.service.RootService;
import com.likeu.word.modules.word.entity.WordEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 词根 Controller
 */
@RestController
@RequestMapping("/root")
public class RootController {

    @Resource
    private RootService rootService;

    /**
     * 词根列表（支持筛选和搜索）
     */
    @GetMapping("/list")
    public Result<PageVO<RootEntity>> list(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return Result.success(rootService.list(type, keyword, page, size));
    }

    /**
     * 词根详情
     */
    @GetMapping("/detail")
    public Result<RootEntity> detail(@RequestParam Long rootId) {
        // 先自增热度再查询，保证响应里的 hot 与库中一致（否则返回值恒比库中少 1）
        rootService.increaseHot(rootId);
        return Result.success(rootService.detail(rootId));
    }

    /**
     * 同源单词列表
     */
    @GetMapping("/words")
    public Result<List<WordEntity>> words(@RequestParam Long rootId) {
        return Result.success(rootService.getWords(rootId));
    }

    /**
     * 查询收藏状态
     */
    @GetMapping("/fav/status")
    public Result<Boolean> favStatus(@RequestAttribute Long userId, @RequestParam Long rootId) {
        return Result.success(rootService.isFavorite(userId, rootId));
    }
}