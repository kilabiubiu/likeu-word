package com.likeu.word.modules.root.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.likeu.word.common.PageVO;
import com.likeu.word.common.util.RedisUtil;
import com.likeu.word.mapper.RootMapper;
import com.likeu.word.mapper.WordMapper;
import com.likeu.word.mapper.WordRootMapper;
import com.likeu.word.modules.favorite.service.FavoriteService;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.root.service.RootService;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.entity.WordRootEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 词根 Service 实现
 *
 * <p>词根属于低频变更的基础数据，列表与同源单词走 Redis 缓存；
 * 缓存以 TTL 作为一致性边界（hot 热度值最多滞后一个 TTL）。</p>
 */
@Slf4j
@Service
public class RootServiceImpl implements RootService {

    private static final String CACHE_ROOT_LIST = "likeu:cache:root:list";
    private static final String CACHE_ROOT_WORDS = "likeu:cache:root:words:";

    /** 词根基础数据缓存时长（分钟） */
    private static final long CACHE_TTL_MINUTES = 60;

    @Resource
    private RootMapper rootMapper;

    @Resource
    private WordRootMapper wordRootMapper;

    @Resource
    private WordMapper wordMapper;

    @Resource
    private FavoriteService favoriteService;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public PageVO<RootEntity> list(Integer type, String keyword, Integer page, Integer size) {
        // 缓存整份词根列表，类型筛选与关键词搜索在内存完成，避免为每种筛选组合各存一份缓存
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<RootEntity> filtered = loadAllRoots().stream()
                .filter(r -> type == null || type <= 0 || type.equals(r.getType()))
                .filter(r -> kw.isEmpty()
                        || (r.getRoot() != null && r.getRoot().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
        return PageVO.fromList(filtered, page, size);
    }

    @Override
    public RootEntity detail(Long rootId) {
        // 详情接口每次访问都会递增热度（写操作），缓存会被立刻置为落后状态，
        // 因此详情不缓存，仅列表与同源单词缓存
        return rootMapper.selectById(rootId);
    }

    @Override
    public List<WordEntity> getWords(Long rootId) {
        String cacheKey = CACHE_ROOT_WORDS + rootId;
        List<WordEntity> cached = readCache(cacheKey, WordEntity.class);
        if (cached != null) {
            return cached;
        }

        List<WordRootEntity> wordRoots = wordRootMapper.selectList(
                new LambdaQueryWrapper<WordRootEntity>()
                        .eq(WordRootEntity::getRootId, rootId));

        List<WordEntity> words;
        if (wordRoots.isEmpty()) {
            words = new ArrayList<>();
        } else {
            List<Long> wordIds = wordRoots.stream()
                    .map(WordRootEntity::getWordId)
                    .collect(Collectors.toList());
            words = wordMapper.selectBatchIds(wordIds);
        }

        writeCache(cacheKey, words);
        return words;
    }

    @Override
    public boolean isFavorite(Long userId, Long rootId) {
        return favoriteService.isRootFav(userId, rootId);
    }

    @Override
    public void increaseHot(Long rootId) {
        rootMapper.update(null,
                new LambdaUpdateWrapper<RootEntity>()
                        .setSql("hot = hot + 1")
                        .eq(RootEntity::getId, rootId));
    }

    /**
     * 读取全部词根，优先命中缓存
     */
    private List<RootEntity> loadAllRoots() {
        List<RootEntity> cached = readCache(CACHE_ROOT_LIST, RootEntity.class);
        if (cached != null) {
            return cached;
        }

        List<RootEntity> roots = rootMapper.selectList(
                new LambdaQueryWrapper<RootEntity>().orderByDesc(RootEntity::getHot));
        writeCache(CACHE_ROOT_LIST, roots);
        return roots;
    }

    /**
     * 读取列表缓存，未命中或反序列化失败时返回 null
     */
    private <T> List<T> readCache(String key, Class<T> elementType) {
        Object cached = redisUtil.get(key);
        if (cached == null) {
            return null;
        }
        try {
            return JSONUtil.toList(cached.toString(), elementType);
        } catch (Exception e) {
            log.warn("词根缓存反序列化失败，将回源数据库: key={}", key, e);
            redisUtil.delete(key);
            return null;
        }
    }

    private void writeCache(String key, List<?> value) {
        redisUtil.set(key, JSONUtil.toJsonStr(value), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }
}
