package com.likeu.word.modules.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likeu.word.common.exception.BusinessException;
import com.likeu.word.mapper.*;
import com.likeu.word.modules.study.dto.StudySubmitDTO;
import com.likeu.word.modules.study.entity.UserDailyEntity;
import com.likeu.word.modules.study.entity.UserWordEntity;
import com.likeu.word.modules.study.service.StudyService;
import com.likeu.word.modules.study.vo.TodayStatsVO;
import com.likeu.word.modules.user.entity.UserEntity;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.entity.WordRootEntity;
import com.likeu.word.modules.word.service.WordService;
import com.likeu.word.modules.word.vo.WordDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习 Service 实现（含SM-2算法核心）
 */
@Slf4j
@Service
public class StudyServiceImpl implements StudyService {

    @Resource
    private UserWordMapper userWordMapper;

    @Resource
    private UserDailyMapper userDailyMapper;

    @Resource
    private WordMapper wordMapper;

    @Resource
    private WordRootMapper wordRootMapper;

    @Resource
    private RootMapper rootMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private WordService wordService;

    /** 默认每日新词上限 */
    private static final int DEFAULT_NEW_LIMIT = 20;

    /** 默认每日复习上限 */
    private static final int DEFAULT_REVIEW_LIMIT = 50;

    @Override
    public List<WordDetailVO> getNewWords(Long userId) {
        // 1. 获取用户当前词书
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getWordBookId() == null) {
            throw new BusinessException("请先选择词书");
        }
        Long bookId = user.getWordBookId();

        // 2. 查询词书中未学过的单词
        // 集合差交给数据库用 NOT EXISTS 完成：若先把已学 word_id 全量读进内存再拼 NOT IN，
        // 已学上千词时会产生超长 IN 列表并放大内存，NOT EXISTS 只依赖唯一键即可判存
        LambdaQueryWrapper<WordEntity> wrapper = new LambdaQueryWrapper<WordEntity>()
                .eq(WordEntity::getWordBookId, bookId)
                .apply("NOT EXISTS (SELECT 1 FROM t_user_word uw "
                        + "WHERE uw.user_id = {0} AND uw.word_id = t_word.id AND uw.deleted = 0)", userId)
                .orderByAsc(WordEntity::getSortOrder);

        // 3. 限制每日新词上限（由 SQL LIMIT 限制，避免全量加载后再截断）
        int limit = user.getDailyNew() != null ? user.getDailyNew() : DEFAULT_NEW_LIMIT;
        Page<WordEntity> page = new Page<>(1, limit);
        page.setSearchCount(false);
        List<WordEntity> words = wordMapper.selectPage(page, wrapper).getRecords();
        if (words.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 批量转换为VO（含词根拆解），避免逐个单词查询
        List<Long> wordIds = words.stream()
                .map(WordEntity::getId)
                .collect(Collectors.toList());
        return wordService.getByIds(wordIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, StudySubmitDTO dto) {
        Long wordId = dto.getWordId();
        Integer quality = dto.getQuality();

        // 1. 查找或创建学习记录
        UserWordEntity record = userWordMapper.selectOne(
                new LambdaQueryWrapper<UserWordEntity>()
                        .eq(UserWordEntity::getUserId, userId)
                        .eq(UserWordEntity::getWordId, wordId));

        // 是否存在有效记录决定本次算「学新词」还是「复习」；
        // 重置进度后旧行被逻辑删除，此时重新学习仍按新词计入统计
        boolean isNew = (record == null);

        if (record == null) {
            // 记录可能因「重置进度」被逻辑删除，唯一键 uk_user_learn_word 决定只能复活旧行（同时清零 SM-2 状态）
            if (userWordMapper.revive(userId, wordId) > 0) {
                record = userWordMapper.selectOne(
                        new LambdaQueryWrapper<UserWordEntity>()
                                .eq(UserWordEntity::getUserId, userId)
                                .eq(UserWordEntity::getWordId, wordId));
            }
        }

        if (record == null) {
            record = new UserWordEntity();
            record.setUserId(userId);
            record.setWordId(wordId);
            record.setStatus(1); // 学习中
            record.setEaseFactor(2.5);
            record.setIntervalDays(0);
            record.setRepetitions(0);
            record.setReviewCount(0);
            record.setWrongCount(0);
        }

        // 2. SM-2算法计算
        sm2Calculate(record, quality);

        // 3. 保存记录
        if (record.getId() == null) {
            userWordMapper.insert(record);
        } else {
            userWordMapper.updateById(record);
        }

        // 4. 更新每日统计
        updateDailyStats(userId, isNew);
    }

    @Override
    public List<WordDetailVO> getReviewWords(Long userId) {
        // 1. 限制每日复习上限（由 SQL LIMIT 限制，避免全量加载后再截断）
        UserEntity user = userMapper.selectById(userId);
        int limit = user.getDailyReview() != null ? user.getDailyReview() : DEFAULT_REVIEW_LIMIT;

        // 2. 查询到期待复习记录
        Date now = new Date();
        Page<UserWordEntity> page = new Page<>(1, limit);
        page.setSearchCount(false);
        List<UserWordEntity> dueRecords = userWordMapper.selectPage(page,
                new LambdaQueryWrapper<UserWordEntity>()
                        .eq(UserWordEntity::getUserId, userId)
                        .eq(UserWordEntity::getStatus, 1) // 学习中
                        .le(UserWordEntity::getDueTime, now)
                        .orderByAsc(UserWordEntity::getDueTime)).getRecords();

        if (dueRecords.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 批量查询单词详情，避免逐个单词查询
        List<Long> wordIds = dueRecords.stream()
                .map(UserWordEntity::getWordId)
                .collect(Collectors.toList());
        return wordService.getByIds(wordIds);
    }

    @Override
    public TodayStatsVO getTodayStats(Long userId) {
        // 查询今日统计
        UserDailyEntity daily = userDailyMapper.selectOne(
                new LambdaQueryWrapper<UserDailyEntity>()
                        .eq(UserDailyEntity::getUserId, userId)
                        .eq(UserDailyEntity::getStudyDate, LocalDate.now()));

        int newCount = daily != null ? daily.getNewCount() : 0;
        int reviewCount = daily != null ? daily.getReviewCount() : 0;

        // 待复习数
        Date now = new Date();
        int dueCount = userWordMapper.selectCount(
                new LambdaQueryWrapper<UserWordEntity>()
                        .eq(UserWordEntity::getUserId, userId)
                        .eq(UserWordEntity::getStatus, 1)
                        .le(UserWordEntity::getDueTime, now)).intValue();

        // 已掌握数
        int masteredCount = userWordMapper.selectCount(
                new LambdaQueryWrapper<UserWordEntity>()
                        .eq(UserWordEntity::getUserId, userId)
                        .eq(UserWordEntity::getStatus, 2)).intValue();

        // 进度百分比（基于每日目标）
        UserEntity user = userMapper.selectById(userId);
        int newLimit = user.getDailyNew() != null ? user.getDailyNew() : DEFAULT_NEW_LIMIT;
        int reviewLimit = user.getDailyReview() != null ? user.getDailyReview() : DEFAULT_REVIEW_LIMIT;
        int totalTarget = newLimit + reviewLimit;
        int totalDone = newCount + reviewCount;
        int progress = totalTarget > 0 ? Math.min(100, totalDone * 100 / totalTarget) : 0;

        return new TodayStatsVO(newCount, reviewCount, dueCount, masteredCount, progress);
    }

    // ==================== SM-2 核心算法 ====================

    /**
     * SM-2间隔重复算法
     *
     * @param record 学习记录（状态会被修改）
     * @param quality 掌握度：1-不认识 3-模糊 5-认识
     */
    private void sm2Calculate(UserWordEntity record, int quality) {
        double ef = record.getEaseFactor() != null ? record.getEaseFactor() : 2.5;
        int reps = record.getRepetitions() != null ? record.getRepetitions() : 0;
        int interval = record.getIntervalDays() != null ? record.getIntervalDays() : 0;

        if (quality >= 3) {
            // 回答正确：递增间隔
            switch (reps) {
                case 0:
                    interval = 1;
                    break;
                case 1:
                    interval = 6;
                    break;
                default:
                    // reps >= 2: interval = interval * EF
                    interval = (int) Math.ceil(interval * ef);
                    break;
            }
            reps++;
        } else {
            // 回答错误：重置间隔
            reps = 0;
            // 不认识(q=1)当天重学，模糊(q=3)1天后复习
            interval = (quality == 1) ? 0 : 1;
        }

        // 更新EF（难度因子）
        double newEf = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        // 限定EF范围 [1.3, 3.0]
        newEf = Math.max(1.3, Math.min(3.0, newEf));

        // 计算下次复习时间
        Calendar cal = Calendar.getInstance();
        if (interval > 0) {
            cal.add(Calendar.DAY_OF_YEAR, interval);
        } else {
            // interval=0 表示当天需要重学，设到当天结束
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
        }
        Date dueTime = cal.getTime();

        // 更新记录
        record.setQuality(quality);
        record.setEaseFactor(newEf);
        record.setIntervalDays(interval);
        record.setRepetitions(reps);
        record.setDueTime(dueTime);
        record.setLastReviewTime(new Date());
        record.setReviewCount(record.getReviewCount() != null ? record.getReviewCount() + 1 : 1);

        if (quality < 3) {
            record.setWrongCount(record.getWrongCount() != null ? record.getWrongCount() + 1 : 1);
        }

        // 判断是否掌握：连续正确次数 >= 5 且 EF >= 2.5
        if (reps >= 5 && newEf >= 2.5) {
            record.setStatus(2); // 已掌握
        } else {
            record.setStatus(1); // 学习中
        }

        log.debug("SM-2计算结果: wordId={}, quality={}, ef={}->{}, interval={}, reps={}, due={}",
                record.getWordId(), quality, String.format("%.2f", ef), newEf, interval, reps, dueTime);
    }

    /**
     * 更新每日学习统计
     */
    private void updateDailyStats(Long userId, boolean isNew) {
        LocalDate today = LocalDate.now();
        UserDailyEntity daily = userDailyMapper.selectOne(
                new LambdaQueryWrapper<UserDailyEntity>()
                        .eq(UserDailyEntity::getUserId, userId)
                        .eq(UserDailyEntity::getStudyDate, today));

        if (daily == null) {
            // 当天统计可能因「重置进度」被逻辑删除，唯一键 uk_user_date 决定只能复活旧行（计数清零）
            if (userDailyMapper.revive(userId, today) > 0) {
                daily = userDailyMapper.selectOne(
                        new LambdaQueryWrapper<UserDailyEntity>()
                                .eq(UserDailyEntity::getUserId, userId)
                                .eq(UserDailyEntity::getStudyDate, today));
            }
        }

        if (daily == null) {
            daily = new UserDailyEntity();
            daily.setUserId(userId);
            daily.setStudyDate(today);
            daily.setNewCount(0);
            daily.setReviewCount(0);
        }

        if (isNew) {
            daily.setNewCount(daily.getNewCount() + 1);
        } else {
            daily.setReviewCount(daily.getReviewCount() + 1);
        }

        if (daily.getId() == null) {
            userDailyMapper.insert(daily);
        } else {
            userDailyMapper.updateById(daily);
        }
    }
}