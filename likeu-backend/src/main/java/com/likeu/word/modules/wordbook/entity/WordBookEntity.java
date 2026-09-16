package com.likeu.word.modules.wordbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 词书表 t_word_book
 */
@Data
@TableName("t_word_book")
public class WordBookEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Integer wordCount;

    private String cover;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}