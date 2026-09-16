package com.likeu.word.modules.word.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单词表 t_word
 */
@Data
@TableName("t_word")
public class WordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wordBookId;

    private String word;

    private String phoneticUk;

    private String phoneticUs;

    private String audioUk;

    private String audioUs;

    private String meaningCn;

    private String exampleEn;

    private String exampleCn;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}