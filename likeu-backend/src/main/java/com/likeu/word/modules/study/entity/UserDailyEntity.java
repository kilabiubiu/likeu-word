package com.likeu.word.modules.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日学习统计表 t_user_daily
 */
@Data
@TableName("t_user_daily")
public class UserDailyEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate studyDate;

    /** 当日学新词数 */
    private Integer newCount;

    /** 当日复习数 */
    private Integer reviewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}