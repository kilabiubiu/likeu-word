package com.likeu.word.common;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一分页返回体
 *
 * <p>所有列表接口统一返回该结构，前端按 {@code records} + {@code total} 实现「加载更多」。</p>
 */
@Data
public class PageVO<T> implements Serializable {

    /** 默认每页条数 */
    public static final int DEFAULT_SIZE = 20;

    /** 每页条数上限，防止前端传入过大 size 拖垮数据库 */
    public static final int MAX_SIZE = 100;

    /** 当前页数据 */
    private List<T> records = new ArrayList<>();

    /** 总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private long page;

    /** 每页条数 */
    private long size;

    public static <T> PageVO<T> of(List<T> records, long total, long page, long size) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records == null ? new ArrayList<>() : records);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setSize(size);
        return vo;
    }

    /** 页码归一化：非法值回落到第 1 页 */
    public static long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    /** 每页条数归一化：非法值回落默认值，超出上限则截断 */
    public static long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
