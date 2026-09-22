package com.likeu.word.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likeu.word.modules.study.entity.UserWordEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户学习记录 Mapper
 */
public interface UserWordMapper extends BaseMapper<UserWordEntity> {

    /**
     * 复活被逻辑删除的学习记录，并把 SM-2 状态重置为初始值
     *
     * <p>表上有唯一键 uk_user_learn_word(user_id, word_id)，重置进度是逻辑删除，
     * 用户重新学习同一单词时不能再 insert，只能把旧行复活并清零学习状态。</p>
     *
     * @return 影响行数，0 表示没有可复活的记录
     */
    @Update("UPDATE t_user_word SET deleted = 0, status = 0, quality = 0, ease_factor = 2.50, "
            + "interval_days = 0, repetitions = 0, due_time = NULL, last_review_time = NULL, "
            + "review_count = 0, wrong_count = 0, update_time = NOW() "
            + "WHERE user_id = #{userId} AND word_id = #{wordId} AND deleted = 1")
    int revive(@Param("userId") Long userId, @Param("wordId") Long wordId);
}
