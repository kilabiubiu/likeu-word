package com.likeu.word.modules.wordbook.controller;

import com.likeu.word.common.Result;
import com.likeu.word.modules.wordbook.entity.WordBookEntity;
import com.likeu.word.modules.wordbook.service.WordBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 词书 Controller
 */
@Tag(name = "词书", description = "词书列表、当前词书与切换")
@Slf4j
@RestController
@RequestMapping("/word-book")
public class WordBookController {

    @Resource
    private WordBookService wordBookService;

    /**
     * 获取词书列表（不分页）
     *
     * <p>词书属极少量基础数据，前端「切换词书」需要一次性展示全部供选择，
     * 因此不做分页；服务端限制最多返回 100 条，避免脏数据导致超大响应。</p>
     */
    @Operation(summary = "获取词书列表", description = "不分页，最多返回 100 条")
    @GetMapping("/list")
    public Result<List<WordBookEntity>> getList() {
        return Result.success(wordBookService.getList());
    }

    /**
     * 获取当前选中词书（需登录）
     */
    @Operation(summary = "获取当前选中词书")
    @GetMapping("/current")
    public Result<WordBookEntity> getCurrent(@RequestAttribute Long userId) {
        return Result.success(wordBookService.getCurrent(userId));
    }

    /**
     * 切换词书
     */
    @Operation(summary = "切换词书")
    @PostMapping("/switch")
    public Result<Void> switchBook(@RequestAttribute Long userId,
                                   @RequestParam Long bookId) {
        wordBookService.switchBook(userId, bookId);
        return Result.success();
    }
}