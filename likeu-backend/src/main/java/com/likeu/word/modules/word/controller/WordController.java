package com.likeu.word.modules.word.controller;

import com.likeu.word.common.Result;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.service.WordService;
import com.likeu.word.modules.word.vo.WordDetailVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 单词 Controller
 */
@RestController
@RequestMapping("/word")
public class WordController {

    @Resource
    private WordService wordService;

    /**
     * 根据词书ID获取单词列表
     */
    @GetMapping("/list")
    public Result<List<WordEntity>> getByBookId(@RequestParam Long bookId) {
        return Result.success(wordService.getByBookId(bookId));
    }

    /**
     * 获取单词详情（含词根拆解）
     */
    @GetMapping("/detail")
    public Result<WordDetailVO> getDetail(@RequestParam Long wordId) {
        return Result.success(wordService.getById(wordId));
    }
}