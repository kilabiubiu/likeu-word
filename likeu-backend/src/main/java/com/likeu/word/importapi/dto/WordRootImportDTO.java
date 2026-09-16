package com.likeu.word.importapi.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 单词-词根关联批量导入DTO
 */
@Data
public class WordRootImportDTO {

    @NotNull(message = "单词ID不能为空")
    private Long wordId;

    @NotNull(message = "词根ID不能为空")
    private Long rootId;

    private Integer position;
}