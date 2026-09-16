package com.likeu.word.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户学习统计VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsVO {

    private Integer totalWords;      // 学习总词
    private Integer masteredWords;   // 已掌握
    private Integer favWords;        // 收藏单词数
    private Integer favRoots;        // 收藏词根数
}