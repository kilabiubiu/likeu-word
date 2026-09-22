package com.likeu.word.importapi.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 单词-词根关联批量导入DTO
 */
@Data
public class WordRootImportDTO {

    @NotNull(message = "单词ID不能为空")
    @Positive(message = "单词ID必须为正整数")
    private Long wordId;

    @NotNull(message = "词根ID不能为空")
    @Positive(message = "词根ID必须为正整数")
    private Long rootId;

    /** 词根在单词中的出现顺序，0 表示未指定 */
    @Min(value = 0, message = "位置顺序不能为负数")
    private Integer position;
}
