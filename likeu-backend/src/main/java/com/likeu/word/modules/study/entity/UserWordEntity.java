package com.likeu.word.modules.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户单词学习记录表 t_user_word（SM-2核心表）
 */
@Data
@TableName("t_user_word")
public class UserWordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long wordId;

    /** 学习状态：0-未学 1-学习中 2-已掌握 */
    private Integer status;

    /** 上次评估质量 1/3/5（不认识/模糊/认识） */
    private Integer quality;

    /** SM-2难度因子EF，初始2.5 */
    private Double easeFactor;

    /** 间隔天数 */
    private Integer intervalDays;

    /** 连续正确次数 */
    private Integer repetitions;

    /** 下次复习到期时间 */
    private Date dueTime;

    /** 上次复习时间 */
    private Date lastReviewTime;

    /** 累计复习次数 */
    private Integer reviewCount;

    /** 累计错误次数 */
    private Integer wrongCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}