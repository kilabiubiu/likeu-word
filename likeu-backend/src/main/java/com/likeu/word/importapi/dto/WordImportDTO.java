package com.likeu.word.importapi.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 单词批量导入DTO
 */
@Data
public class WordImportDTO {

    @NotBlank(message = "单词不能为空")
    private String word;

    private String phoneticUk;
    private String phoneticUs;
    private String audioUk;
    private String audioUs;

    @NotBlank(message = "中文释义不能为空")
    private String meaningCn;

    private String exampleEn;
    private String exampleCn;
    private Integer sortOrder;
}