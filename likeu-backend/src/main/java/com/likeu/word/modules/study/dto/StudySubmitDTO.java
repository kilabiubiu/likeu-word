package com.likeu.word.modules.study.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 提交掌握度请求DTO
 */
@Data
public class StudySubmitDTO {

    @NotNull(message = "单词ID不能为空")
    private Long wordId;

    /**
     * 掌握度：
     * 1 - 不认识
     * 3 - 模糊
     * 5 - 认识
     */
    @NotNull(message = "掌握度不能为空")
    @Min(value = 1, message = "掌握度最少为1")
    @Max(value = 5, message = "掌握度最多为5")
    private Integer quality;
}