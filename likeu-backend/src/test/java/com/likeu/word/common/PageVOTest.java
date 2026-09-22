package com.likeu.word.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页参数归一化与返回体边界单测
 */
class PageVOTest {

    @ParameterizedTest(name = "page={0} -> {1}")
    @CsvSource({
            "1, 1",
            "5, 5",
            "0, 1",
            "-3, 1"
    })
    @DisplayName("页码归一化：非法值回落到第 1 页")
    void normalizePage(Integer page, long expected) {
        assertEquals(expected, PageVO.normalizePage(page));
    }

    @Test
    @DisplayName("页码为 null 时回落到第 1 页")
    void normalizeNullPage() {
        assertEquals(1, PageVO.normalizePage(null));
    }

    @ParameterizedTest(name = "size={0} -> {1}")
    @CsvSource({
            "20, 20",
            "1, 1",
            "100, 100",
            "101, 100",
            "10000, 100",
            "0, 20",
            "-1, 20"
    })
    @DisplayName("每页条数归一化：非法值回落默认值，超出上限则截断")
    void normalizeSize(Integer size, long expected) {
        assertEquals(expected, PageVO.normalizeSize(size));
    }

    @Test
    @DisplayName("每页条数为 null 时回落默认值 20")
    void normalizeNullSize() {
        assertEquals(PageVO.DEFAULT_SIZE, PageVO.normalizeSize(null));
        assertEquals(20, PageVO.DEFAULT_SIZE);
        assertEquals(100, PageVO.MAX_SIZE);
    }

    @Test
    @DisplayName("of：records 为 null 时返回空列表而非 null，避免调用方空指针")
    void ofWithNullRecords() {
        PageVO<String> vo = PageVO.of(null, 0, 1, 20);

        assertTrue(vo.getRecords().isEmpty());
        assertEquals(0, vo.getTotal());
        assertEquals(1, vo.getPage());
        assertEquals(20, vo.getSize());
    }

    @Test
    @DisplayName("of：如实透传 records/total/page/size")
    void ofKeepsValues() {
        List<String> records = Arrays.asList("a", "b");

        PageVO<String> vo = PageVO.of(records, 7, 2, 2);

        assertEquals(records, vo.getRecords());
        assertEquals(7, vo.getTotal());
        assertEquals(2, vo.getPage());
        assertEquals(2, vo.getSize());
    }
}
