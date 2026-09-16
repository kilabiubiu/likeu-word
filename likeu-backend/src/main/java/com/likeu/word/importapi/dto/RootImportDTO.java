package com.likeu.word.importapi.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 词根词缀批量导入DTO
 */
@Data
public class RootImportDTO {

    @NotNull(message = "类型不能为空")
    private Integer type; // 1-前缀 2-词根 3-后缀

    @NotBlank(message = "词根文本不能为空")
    private String root;

    @NotBlank(message = "含义不能为空")
    private String meaning;

    private String origin;
    private String example;
}