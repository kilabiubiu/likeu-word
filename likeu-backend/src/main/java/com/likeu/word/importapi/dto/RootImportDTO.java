package com.likeu.word.importapi.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 词根词缀批量导入DTO
 *
 * <p>字段长度与 {@code schema.sql} 中 t_root 的列定义对齐，避免超长才在 DB 层报错。</p>
 */
@Data
public class RootImportDTO {

    /** 1-前缀 2-词根 3-后缀 */
    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型只能是 1-前缀 2-词根 3-后缀")
    @Max(value = 3, message = "类型只能是 1-前缀 2-词根 3-后缀")
    private Integer type;

    @NotBlank(message = "词根文本不能为空")
    @Size(max = 64, message = "词根文本长度不能超过64")
    private String root;

    @NotBlank(message = "含义不能为空")
    @Size(max = 128, message = "含义长度不能超过128")
    private String meaning;

    @Size(max = 64, message = "来源长度不能超过64")
    private String origin;

    @Size(max = 512, message = "示例长度不能超过512")
    private String example;
}
