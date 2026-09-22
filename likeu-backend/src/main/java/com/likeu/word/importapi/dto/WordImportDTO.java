package com.likeu.word.importapi.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 单词批量导入DTO
 *
 * <p>字段长度与 {@code schema.sql} 中 t_word 的列定义对齐，避免超长才在 DB 层报错。</p>
 */
@Data
public class WordImportDTO {

    /** 所属词书，与 {@code word} 共同构成判存用的自然键 */
    @NotNull(message = "词书ID不能为空")
    private Long wordBookId;

    @NotBlank(message = "单词不能为空")
    @Size(max = 64, message = "单词长度不能超过64")
    private String word;

    @Size(max = 64, message = "英式音标长度不能超过64")
    private String phoneticUk;

    @Size(max = 64, message = "美式音标长度不能超过64")
    private String phoneticUs;

    @Size(max = 512, message = "英式发音地址长度不能超过512")
    private String audioUk;

    @Size(max = 512, message = "美式发音地址长度不能超过512")
    private String audioUs;

    @NotBlank(message = "中文释义不能为空")
    private String meaningCn;

    private String exampleEn;

    private String exampleCn;

    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder;
}
