package com.likeu.word.modules.favorite.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏单词表 t_user_fav_word
 */
@Data
@TableName("t_user_fav_word")
public class UserFavWordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long wordId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}