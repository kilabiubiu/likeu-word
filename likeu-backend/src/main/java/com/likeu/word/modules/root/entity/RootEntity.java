package com.likeu.word.modules.root.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 词根词缀表 t_root
 */
@Data
@TableName("t_root")
public class RootEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer type; // 1-前缀 2-词根 3-后缀

    private String root;

    private String meaning;

    private String origin;

    private String example;

    private Integer hot;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}