package com.likeu.word.modules.study.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 今日学习统计VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayStatsVO {

    /** 今日新学词数 */
    private Integer newCount;

    /** 今日复习数 */
    private Integer reviewCount;

    /** 待复习数 */
    private Integer dueCount;

    /** 已掌握词数 */
    private Integer masteredCount;

    /** 学习进度百分比 */
    private Integer progress;

    /** 每日新词上限（来源 t_user.daily_new），供前端展示，避免前端写死 */
    private Integer dailyNewLimit;
}