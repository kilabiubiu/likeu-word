package com.likeu.word.modules.root.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likeu.word.common.PageVO;
import com.likeu.word.common.util.CacheHelper;
import com.likeu.word.mapper.RootMapper;
import com.likeu.word.mapper.WordMapper;
import com.likeu.word.mapper.WordRootMapper;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.root.service.RootService;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.entity.WordRootEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 词根 Service 实现
 *
 * <p>列表走数据库真分页 + {@code idx_type_hot} 索引，不做缓存：热度随每次详情访问变化，
 * 缓存列表既无法命中（筛选组合多）又需要频繁失效。</p>
 *
 * <p>同源单词属低频变更数据，按 rootId 走 Redis 缓存，TTL 作为一致性边界。</p>
 */
@Slf4j
@Service
public class RootServiceImpl implements RootService {

    private static final String CACHE_ROOT_WORDS = "likeu:cache:root:words:";

    /** 词根基础数据缓存时长（分钟），可通过配置覆盖 */
    @Value("${likeu.cache.root-ttl-minutes:60}")
    private long cacheTtlMinutes;

    @Resource
    private RootMapper rootMapper;

    @Resource
    private WordRootMapper wordRootMapper;

    @Resource
    private WordMapper wordMapper;

    @Resource
    private CacheHelper cacheHelper;

    @Override
    public PageVO<RootEntity> list(Integer type, String keyword, Integer page, Integer size) {
        long pageNum = PageVO.normalizePage(page);
        long pageSize = PageVO.normalizeSize(size);

        LambdaQueryWrapper<RootEntity> wrapper = new LambdaQueryWrapper<>();
        // type <= 0 视为不筛选（前端「全部」传 0），与旧的内存过滤语义保持一致
        if (type != null && type > 0) {
            wrapper.eq(RootEntity::getType, type);
        }
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            // 词根文本均为小写，统一小写后匹配，保持旧逻辑「输入大小写不敏感」的行为
            wrapper.like(RootEntity::getRoot, kw.toLowerCase());
        }
        wrapper.orderByDesc(RootEntity::getHot);

        Page<RootEntity> result = rootMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageVO.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public RootEntity detail(Long rootId) {
        // 详情接口每次访问都会递增热度（写操作），缓存会被立刻置为落后状态，因此详情不缓存
        return rootMapper.selectById(rootId);
    }

    @Override
    public List<WordEntity> getWords(Long rootId) {
        return cacheHelper.getList(CACHE_ROOT_WORDS + rootId, WordEntity.class,
                cacheTtlMinutes * 60, () -> queryWords(rootId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseHot(Long rootId) {
        // 列表已改为数据库真分页且不缓存，热度变化无需再失效任何缓存
        rootMapper.update(null,
                new LambdaUpdateWrapper<RootEntity>()
                        .setSql("hot = hot + 1")
                        .eq(RootEntity::getId, rootId));
    }

    /**
     * 查询同源单词：一次关联查询 + 一次批量查单词，避免逐个单词查询
     */
    private List<WordEntity> queryWords(Long rootId) {
        List<WordRootEntity> wordRoots = wordRootMapper.selectList(
                new LambdaQueryWrapper<WordRootEntity>()
                        .eq(WordRootEntity::getRootId, rootId));
        if (wordRoots.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> wordIds = wordRoots.stream()
                .map(WordRootEntity::getWordId)
                .collect(Collectors.toList());
        return wordMapper.selectBatchIds(wordIds);
    }
}
