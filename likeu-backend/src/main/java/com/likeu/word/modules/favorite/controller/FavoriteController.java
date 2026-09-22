package com.likeu.word.modules.favorite.controller;

import com.likeu.word.common.PageVO;
import com.likeu.word.common.Result;
import com.likeu.word.modules.favorite.service.FavoriteService;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.word.entity.WordEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 收藏 Controller
 */
@Tag(name = "收藏", description = "单词收藏与词根收藏")
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    // ========== 单词收藏 ==========

    @Operation(summary = "收藏单词列表")
    @GetMapping("/word/list")
    public Result<PageVO<WordEntity>> wordFavList(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(favoriteService.listFavWords(userId, page, size));
    }

    @Operation(summary = "收藏单词")
    @PostMapping("/word")
    public Result<Void> addWordFav(@RequestAttribute Long userId, @RequestParam Long wordId) {
        favoriteService.addWordFav(userId, wordId);
        return Result.success();
    }

    @Operation(summary = "取消收藏单词")
    @DeleteMapping("/word")
    public Result<Void> removeWordFav(@RequestAttribute Long userId, @RequestParam Long wordId) {
        favoriteService.removeWordFav(userId, wordId);
        return Result.success();
    }

    @Operation(summary = "查询单词是否已收藏")
    @GetMapping("/word/status")
    public Result<Boolean> wordFavStatus(@RequestAttribute Long userId, @RequestParam Long wordId) {
        return Result.success(favoriteService.isWordFav(userId, wordId));
    }

    // ========== 词根收藏 ==========

    @Operation(summary = "收藏词根列表")
    @GetMapping("/root/list")
    public Result<PageVO<RootEntity>> rootFavList(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(favoriteService.listFavRoots(userId, page, size));
    }

    @Operation(summary = "收藏词根")
    @PostMapping("/root")
    public Result<Void> addRootFav(@RequestAttribute Long userId, @RequestParam Long rootId) {
        favoriteService.addRootFav(userId, rootId);
        return Result.success();
    }

    @Operation(summary = "取消收藏词根")
    @DeleteMapping("/root")
    public Result<Void> removeRootFav(@RequestAttribute Long userId, @RequestParam Long rootId) {
        favoriteService.removeRootFav(userId, rootId);
        return Result.success();
    }

    @Operation(summary = "查询词根是否已收藏")
    @GetMapping("/root/status")
    public Result<Boolean> rootFavStatus(@RequestAttribute Long userId, @RequestParam Long rootId) {
        return Result.success(favoriteService.isRootFav(userId, rootId));
    }
}
