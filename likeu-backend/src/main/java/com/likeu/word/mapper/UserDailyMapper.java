package com.likeu.word.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likeu.word.modules.study.entity.UserDailyEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 每日统计 Mapper
 */
public interface UserDailyMapper extends BaseMapper<UserDailyEntity> {

    /**
     * 复活被逻辑删除的当日统计，并把计数清零
     *
     * <p>表上有唯一键 uk_user_date(user_id, study_date)，重置进度后当天再学习时
     * 不能再 insert，只能复活旧行重新计数。</p>
     *
     * @return 影响行数，0 表示没有可复活的记录
     */
    @Update("UPDATE t_user_daily SET deleted = 0, new_count = 0, review_count = 0, update_time = NOW() "
            + "WHERE user_id = #{userId} AND study_date = #{studyDate} AND deleted = 1")
    int revive(@Param("userId") Long userId, @Param("studyDate") LocalDate studyDate);
}
