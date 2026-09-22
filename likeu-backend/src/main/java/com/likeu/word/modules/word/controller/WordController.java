package com.likeu.word.modules.word.controller;

import com.likeu.word.common.PageVO;
import com.likeu.word.common.Result;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.service.WordService;
import com.likeu.word.modules.word.vo.WordDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 单词 Controller
 */
@Tag(name = "单词", description = "单词列表与详情（含词根拆解）")
@RestController
@RequestMapping("/word")
public class WordController {

    @Resource
    private WordService wordService;

    /**
     * 根据词书ID分页获取单词列表
     */
    @Operation(summary = "根据词书分页获取单词列表")
    @GetMapping("/list")
    public Result<PageVO<WordEntity>> getByBookId(
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return Result.success(wordService.getByBookId(bookId, page, size));
    }

    /**
     * 获取单词详情（含词根拆解）
     */
    @Operation(summary = "获取单词详情", description = "返回单词释义与词根词缀拆解")
    @GetMapping("/detail")
    public Result<WordDetailVO> getDetail(@RequestParam Long wordId) {
        return Result.success(wordService.getById(wordId));
    }
}
