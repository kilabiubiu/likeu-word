package com.likeu.word.importapi.vo;

import lombok.Data;

/**
 * 批量导入结果统计
 */
@Data
public class ImportResultVO {

    /** 新增行数 */
    private int inserted;

    /** 命中已存在行并更新行数 */
    private int updated;

    /** 批次内重复而跳过的行数 */
    private int skipped;

    public static ImportResultVO of(int inserted, int updated, int skipped) {
        ImportResultVO vo = new ImportResultVO();
        vo.setInserted(inserted);
        vo.setUpdated(updated);
        vo.setSkipped(skipped);
        return vo;
    }
}
