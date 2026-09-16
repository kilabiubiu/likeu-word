package com.likeu.word.modules.wordbook.controller;

import com.likeu.word.common.Result;
import com.likeu.word.modules.wordbook.entity.WordBookEntity;
import com.likeu.word.modules.wordbook.service.WordBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 词书 Controller
 */
@Slf4j
@RestController
@RequestMapping("/word-book")
public class WordBookController {

    @Resource
    private WordBookService wordBookService;

    /**
     * 获取词书列表
     */
    @GetMapping("/list")
    public Result<List<WordBookEntity>> getList() {
        return Result.success(wordBookService.getList());
    }

    /**
     * 获取当前选中词书（需登录）
     */
    @GetMapping("/current")
    public Result<WordBookEntity> getCurrent(@RequestAttribute Long userId) {
        return Result.success(wordBookService.getCurrent(userId));
    }

    /**
     * 切换词书
     */
    @PostMapping("/switch")
    public Result<Void> switchBook(@RequestAttribute Long userId,
                                   @RequestParam Long bookId) {
        wordBookService.switchBook(userId, bookId);
        return Result.success();
    }
}