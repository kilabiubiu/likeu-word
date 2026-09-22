package com.likeu.word.importapi.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 单词批量导入请求体
 *
 * <p>Spring MVC 不会校验 {@code @RequestBody List<T>} 的元素级约束，
 * 必须用请求体包一层并在字段上标 {@code @Valid} 才能级联到每个元素。</p>
 */
@Data
public class WordImportRequestDTO {

    @NotEmpty(message = "导入数据不能为空")
    @Valid
    private List<WordImportDTO> items;
}
