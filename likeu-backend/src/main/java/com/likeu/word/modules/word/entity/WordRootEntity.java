package com.likeu.word.modules.word.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单词-词根关联表 t_word_root
 */
@Data
@TableName("t_word_root")
public class WordRootEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wordId;

    private Long rootId;

    private Integer position;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}