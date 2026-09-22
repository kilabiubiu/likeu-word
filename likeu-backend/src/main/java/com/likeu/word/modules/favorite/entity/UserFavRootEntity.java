package com.likeu.word.modules.favorite.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏词根表 t_user_fav_root
 */
@Data
@TableName("t_user_fav_root")
public class UserFavRootEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long rootId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}