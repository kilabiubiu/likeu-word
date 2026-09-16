package com.likeu.word.modules.study.service;

import com.likeu.word.modules.study.dto.StudySubmitDTO;
import com.likeu.word.modules.study.vo.TodayStatsVO;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.vo.WordDetailVO;

import java.util.List;

/**
 * 学习 Service 接口
 */
public interface StudyService {

    /**
     * 获取新词列表（当前词书中未学过的单词）
     */
    List<WordDetailVO> getNewWords(Long userId);

    /**
     * 提交掌握度（SM-2算法核心）
     */
    void submit(Long userId, StudySubmitDTO dto);

    /**
     * 获取待复习单词列表（due_time <= now）
     */
    List<WordDetailVO> getReviewWords(Long userId);

    /**
     * 获取今日学习统计
     */
    TodayStatsVO getTodayStats(Long userId);
}