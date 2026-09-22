package com.likeu.word.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.likeu.word.modules.favorite.entity.UserFavWordEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 收藏单词 Mapper
 */
public interface UserFavWordMapper extends BaseMapper<UserFavWordEntity> {

    /**
     * 复活被逻辑删除的收藏记录，并刷新收藏时间
     *
     * <p>表上有唯一键 uk_user_fav_word(user_id, word_id)，取消收藏是逻辑删除，
     * 再次收藏同一单词时不能再 insert，只能复活旧行。</p>
     *
     * @return 影响行数，0 表示没有可复活的记录
     */
    @Update("UPDATE t_user_fav_word SET deleted = 0, create_time = NOW() "
            + "WHERE user_id = #{userId} AND word_id = #{wordId} AND deleted = 1")
    int revive(@Param("userId") Long userId, @Param("wordId") Long wordId);
}
