package com.likeu.word.modules.favorite.controller;

import com.likeu.word.common.PageVO;
import com.likeu.word.common.Result;
import com.likeu.word.modules.favorite.service.FavoriteService;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.word.entity.WordEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 收藏 Controller
 */
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    // ========== 单词收藏 ==========

    @GetMapping("/word/list")
    public Result<PageVO<WordEntity>> wordFavList(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(favoriteService.listFavWords(userId, page, size));
    }

    @PostMapping("/word")
    public Result<Void> addWordFav(@RequestAttribute Long userId, @RequestParam Long wordId) {
        favoriteService.addWordFav(userId, wordId);
        return Result.success();
    }

    @DeleteMapping("/word")
    public Result<Void> removeWordFav(@RequestAttribute Long userId, @RequestParam Long wordId) {
        favoriteService.removeWordFav(userId, wordId);
        return Result.success();
    }

    @GetMapping("/word/status")
    public Result<Boolean> wordFavStatus(@RequestAttribute Long userId, @RequestParam Long wordId) {
        return Result.success(favoriteService.isWordFav(userId, wordId));
    }

    // ========== 词根收藏 ==========

    @GetMapping("/root/list")
    public Result<PageVO<RootEntity>> rootFavList(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(favoriteService.listFavRoots(userId, page, size));
    }

    @PostMapping("/root")
    public Result<Void> addRootFav(@RequestAttribute Long userId, @RequestParam Long rootId) {
        favoriteService.addRootFav(userId, rootId);
        return Result.success();
    }

    @DeleteMapping("/root")
    public Result<Void> removeRootFav(@RequestAttribute Long userId, @RequestParam Long rootId) {
        favoriteService.removeRootFav(userId, rootId);
        return Result.success();
    }

    @GetMapping("/root/status")
    public Result<Boolean> rootFavStatus(@RequestAttribute Long userId, @RequestParam Long rootId) {
        return Result.success(favoriteService.isRootFav(userId, rootId));
    }
}