package com.likeu.word.modules.study.service.impl;

import com.likeu.word.modules.study.entity.UserWordEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SM-2 算法单测
 *
 * <p>固化三处有意偏离（EF 上限 3.0、q=1 当天重学、interval 向上取整），
 * 防止后续按「标准 SM-2」重构时无声改变用户既有复习节奏。</p>
 */
class Sm2CalculateTest {

    private final StudyServiceImpl service = new StudyServiceImpl();

    /** 构造一条初始学习记录：初始 EF 2.5、reps 0、interval 0 */
    private UserWordEntity newRecord() {
        UserWordEntity record = new UserWordEntity();
        record.setWordId(1L);
        record.setUserId(1L);
        record.setStatus(1);
        record.setEaseFactor(2.5);
        record.setRepetitions(0);
        record.setIntervalDays(0);
        record.setReviewCount(0);
        record.setWrongCount(0);
        return record;
    }

    @ParameterizedTest(name = "quality={0} -> EF={1}")
    @CsvSource({
            "1, 1.96",
            "2, 2.18",
            "3, 2.36",
            "4, 2.50",
            "5, 2.60"
    })
    @DisplayName("EF 按 SM-2 公式调整，并定点保留两位小数")
    void efFollowsFormula(int quality, double expectedEf) {
        UserWordEntity record = newRecord();

        service.sm2Calculate(record, quality);

        assertEquals(expectedEf, record.getEaseFactor(), 1e-9);
        assertEquals(quality, record.getQuality());
        assertNotNull(record.getDueTime());
        assertNotNull(record.getLastReviewTime());
    }

    @Test
    @DisplayName("EF 下界锁定 1.3")
    void efLowerBoundIsClamped() {
        UserWordEntity record = newRecord();
        record.setEaseFactor(1.3);

        service.sm2Calculate(record, 1);

        assertEquals(1.3, record.getEaseFactor(), 1e-9);
    }

    @Test
    @DisplayName("EF 上界锁定 3.0（有意偏离标准 SM-2）")
    void efUpperBoundIsClamped() {
        UserWordEntity record = newRecord();
        record.setEaseFactor(2.95);

        service.sm2Calculate(record, 5);

        assertEquals(3.0, record.getEaseFactor(), 1e-9);
    }

    @Test
    @DisplayName("首次答对：reps 0->1，interval 0->1")
    void firstCorrectAnswerSetsIntervalToOneDay() {
        UserWordEntity record = newRecord();

        service.sm2Calculate(record, 5);

        assertEquals(1, record.getRepetitions());
        assertEquals(1, record.getIntervalDays());
        assertEquals(1, record.getReviewCount());
        assertEquals(0, record.getWrongCount());
        assertEquals(1, record.getStatus());
    }

    @Test
    @DisplayName("第二次答对：interval 固定为 6 天")
    void secondCorrectAnswerSetsIntervalToSixDays() {
        UserWordEntity record = newRecord();
        record.setRepetitions(1);
        record.setIntervalDays(1);

        service.sm2Calculate(record, 5);

        assertEquals(2, record.getRepetitions());
        assertEquals(6, record.getIntervalDays());
    }

    @Test
    @DisplayName("第三次起：interval = ceil(interval * EF)，向上取整（12.5 -> 13）")
    void laterCorrectAnswerMultipliesIntervalWithCeil() {
        UserWordEntity record = newRecord();
        record.setRepetitions(2);
        record.setIntervalDays(5);

        service.sm2Calculate(record, 5);

        assertEquals(3, record.getRepetitions());
        assertEquals(13, record.getIntervalDays());
    }

    @Test
    @DisplayName("答错（q=1）：reps 归零、当天重学、错误次数 +1")
    void wrongAnswerResetsRepetitionsAndStudiesToday() {
        UserWordEntity record = newRecord();
        record.setRepetitions(4);
        record.setIntervalDays(30);
        record.setReviewCount(6);

        service.sm2Calculate(record, 1);

        assertEquals(0, record.getRepetitions());
        assertEquals(0, record.getIntervalDays());
        assertEquals(7, record.getReviewCount());
        assertEquals(1, record.getWrongCount());
        assertEquals(1, record.getStatus());

        Calendar due = Calendar.getInstance();
        due.setTime(record.getDueTime());
        Calendar today = Calendar.getInstance();
        assertEquals(today.get(Calendar.DAY_OF_YEAR), due.get(Calendar.DAY_OF_YEAR));
        assertEquals(23, due.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, due.get(Calendar.MINUTE));
    }

    @Test
    @DisplayName("模糊（q=3）按答对处理：reps 递增、间隔按 EF 放大、错误次数不变")
    void fuzzyAnswerCountsAsCorrect() {
        UserWordEntity record = newRecord();
        record.setRepetitions(3);
        record.setIntervalDays(20);

        service.sm2Calculate(record, 3);

        assertEquals(4, record.getRepetitions());
        assertEquals(50, record.getIntervalDays());
        assertEquals(2.36, record.getEaseFactor(), 1e-9);
        assertEquals(0, record.getWrongCount());
        assertEquals(1, record.getStatus());
    }

    @Test
    @DisplayName("q=2 视为答错：reps 归零、1 天后复习、错误次数 +1")
    void qualityTwoTreatsAsWrong() {
        UserWordEntity record = newRecord();
        record.setRepetitions(3);
        record.setIntervalDays(20);

        service.sm2Calculate(record, 2);

        assertEquals(0, record.getRepetitions());
        assertEquals(1, record.getIntervalDays());
        assertEquals(2.18, record.getEaseFactor(), 1e-9);
        assertEquals(1, record.getWrongCount());
    }

    @Test
    @DisplayName("连续正确 >= 5 次且 EF >= 2.5 判定为已掌握")
    void masteredWhenRepsAndEfReachThreshold() {
        UserWordEntity record = newRecord();
        record.setRepetitions(5);
        record.setIntervalDays(30);

        service.sm2Calculate(record, 5);

        assertEquals(6, record.getRepetitions());
        assertEquals(2, record.getStatus());
    }

    @Test
    @DisplayName("reps 达标但 EF 低于 2.5 仍为学习中")
    void notMasteredWhenEfBelowThreshold() {
        UserWordEntity record = newRecord();
        record.setRepetitions(4);
        record.setIntervalDays(20);
        record.setEaseFactor(2.4);

        service.sm2Calculate(record, 3);

        assertEquals(5, record.getRepetitions());
        assertEquals(1, record.getStatus());
    }

    @Test
    @DisplayName("SM-2 字段为 null 时按初始值（EF 2.5 / reps 0 / interval 0）处理")
    void nullFieldsFallBackToInitialValues() {
        UserWordEntity record = new UserWordEntity();
        record.setWordId(9L);

        service.sm2Calculate(record, 5);

        assertEquals(2.6, record.getEaseFactor(), 1e-9);
        assertEquals(1, record.getRepetitions());
        assertEquals(1, record.getIntervalDays());
        assertEquals(1, record.getReviewCount());
        assertNull(record.getWrongCount());
    }

    @Test
    @DisplayName("上次复习时间被刷新为当前时间")
    void lastReviewTimeIsRefreshed() {
        UserWordEntity record = newRecord();
        Date before = new Date();

        service.sm2Calculate(record, 3);

        assertFalse(record.getLastReviewTime().before(before));
    }
}
