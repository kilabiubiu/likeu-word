package com.likeu.word.modules.study.controller;

import com.likeu.word.common.Result;
import com.likeu.word.modules.study.dto.StudySubmitDTO;
import com.likeu.word.modules.study.service.StudyService;
import com.likeu.word.modules.study.vo.TodayStatsVO;
import com.likeu.word.modules.word.vo.WordDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 学习 Controller
 */
@Slf4j
@RestController
@RequestMapping("/study")
public class StudyController {

    @Resource
    private StudyService studyService;

    /**
     * 获取新词列表
     */
    @GetMapping("/new-words")
    public Result<List<WordDetailVO>> getNewWords(@RequestAttribute Long userId) {
        return Result.success(studyService.getNewWords(userId));
    }

    /**
     * 提交掌握度（SM-2算法）
     */
    @PostMapping("/submit")
    public Result<Void> submit(@RequestAttribute Long userId,
                               @Valid @RequestBody StudySubmitDTO dto) {
        studyService.submit(userId, dto);
        return Result.success();
    }

    /**
     * 获取待复习单词
     */
    @GetMapping("/review-words")
    public Result<List<WordDetailVO>> getReviewWords(@RequestAttribute Long userId) {
        return Result.success(studyService.getReviewWords(userId));
    }

    /**
     * 今日学习统计
     */
    @GetMapping("/stats/today")
    public Result<TodayStatsVO> getTodayStats(@RequestAttribute Long userId) {
        return Result.success(studyService.getTodayStats(userId));
    }
}