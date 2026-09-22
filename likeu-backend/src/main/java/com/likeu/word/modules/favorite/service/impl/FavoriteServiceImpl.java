package com.likeu.word.modules.favorite.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likeu.word.common.PageVO;
import com.likeu.word.mapper.RootMapper;
import com.likeu.word.mapper.UserFavRootMapper;
import com.likeu.word.mapper.UserFavWordMapper;
import com.likeu.word.mapper.WordMapper;
import com.likeu.word.modules.favorite.entity.UserFavRootEntity;
import com.likeu.word.modules.favorite.entity.UserFavWordEntity;
import com.likeu.word.modules.favorite.service.FavoriteService;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.word.entity.WordEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏 Service 实现
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Resource
    private UserFavWordMapper userFavWordMapper;

    @Resource
    private UserFavRootMapper userFavRootMapper;

    @Resource
    private WordMapper wordMapper;

    @Resource
    private RootMapper rootMapper;

    // ==================== 单词收藏 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWordFav(Long userId, Long wordId) {
        Long count = userFavWordMapper.selectCount(
                new LambdaQueryWrapper<UserFavWordEntity>()
                        .eq(UserFavWordEntity::getUserId, userId)
                        .eq(UserFavWordEntity::getWordId, wordId));
        if (count == 0) {
            // 取消收藏是逻辑删除，唯一键 uk_user_fav_word 决定再次收藏只能复活旧行
            if (userFavWordMapper.revive(userId, wordId) == 0) {
                UserFavWordEntity fav = new UserFavWordEntity();
                fav.setUserId(userId);
                fav.setWordId(wordId);
                userFavWordMapper.insert(fav);
            }
            log.info("用户收藏单词: userId={}, wordId={}", userId, wordId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWordFav(Long userId, Long wordId) {
        userFavWordMapper.delete(
                new LambdaQueryWrapper<UserFavWordEntity>()
                        .eq(UserFavWordEntity::getUserId, userId)
                        .eq(UserFavWordEntity::getWordId, wordId));
        log.info("用户取消收藏单词: userId={}, wordId={}", userId, wordId);
    }

    @Override
    public boolean isWordFav(Long userId, Long wordId) {
        Long count = userFavWordMapper.selectCount(
                new LambdaQueryWrapper<UserFavWordEntity>()
                        .eq(UserFavWordEntity::getUserId, userId)
                        .eq(UserFavWordEntity::getWordId, wordId));
        return count > 0;
    }

    @Override
    public int countWordFav(Long userId) {
        return userFavWordMapper.selectCount(
                new LambdaQueryWrapper<UserFavWordEntity>()
                        .eq(UserFavWordEntity::getUserId, userId)).intValue();
    }

    @Override
    public PageVO<WordEntity> listFavWords(Long userId, Integer page, Integer size) {
        // 先对收藏记录分页，再批量取单词，避免把全部收藏记录与单词都加载进内存
        Page<UserFavWordEntity> pageParam = new Page<>(
                PageVO.normalizePage(page), PageVO.normalizeSize(size));
        Page<UserFavWordEntity> favPage = userFavWordMapper.selectPage(pageParam,
                new LambdaQueryWrapper<UserFavWordEntity>()
                        .eq(UserFavWordEntity::getUserId, userId)
                        .orderByDesc(UserFavWordEntity::getCreateTime)
                        .orderByDesc(UserFavWordEntity::getId));

        List<Long> wordIds = selectFavIds(favPage.getRecords(), UserFavWordEntity::getWordId);
        if (wordIds.isEmpty()) {
            return PageVO.of(new ArrayList<>(), favPage.getTotal(),
                    favPage.getCurrent(), favPage.getSize());
        }

        Map<Long, WordEntity> wordMap = wordMapper.selectBatchIds(wordIds).stream()
                .collect(Collectors.toMap(WordEntity::getId, Function.identity()));
        return PageVO.of(sortByIds(wordIds, wordMap), favPage.getTotal(),
                favPage.getCurrent(), favPage.getSize());
    }

    // ==================== 词根收藏 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRootFav(Long userId, Long rootId) {
        Long count = userFavRootMapper.selectCount(
                new LambdaQueryWrapper<UserFavRootEntity>()
                        .eq(UserFavRootEntity::getUserId, userId)
                        .eq(UserFavRootEntity::getRootId, rootId));
        if (count == 0) {
            // 取消收藏是逻辑删除，唯一键 uk_user_root 决定再次收藏只能复活旧行
            if (userFavRootMapper.revive(userId, rootId) == 0) {
                UserFavRootEntity fav = new UserFavRootEntity();
                fav.setUserId(userId);
                fav.setRootId(rootId);
                userFavRootMapper.insert(fav);
            }
            log.info("用户收藏词根: userId={}, rootId={}", userId, rootId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRootFav(Long userId, Long rootId) {
        userFavRootMapper.delete(
                new LambdaQueryWrapper<UserFavRootEntity>()
                        .eq(UserFavRootEntity::getUserId, userId)
                        .eq(UserFavRootEntity::getRootId, rootId));
        log.info("用户取消收藏词根: userId={}, rootId={}", userId, rootId);
    }

    @Override
    public boolean isRootFav(Long userId, Long rootId) {
        Long count = userFavRootMapper.selectCount(
                new LambdaQueryWrapper<UserFavRootEntity>()
                        .eq(UserFavRootEntity::getUserId, userId)
                        .eq(UserFavRootEntity::getRootId, rootId));
        return count > 0;
    }

    @Override
    public int countRootFav(Long userId) {
        return userFavRootMapper.selectCount(
                new LambdaQueryWrapper<UserFavRootEntity>()
                        .eq(UserFavRootEntity::getUserId, userId)).intValue();
    }

    @Override
    public PageVO<RootEntity> listFavRoots(Long userId, Integer page, Integer size) {
        // 先对收藏记录分页，再批量取词根
        Page<UserFavRootEntity> pageParam = new Page<>(
                PageVO.normalizePage(page), PageVO.normalizeSize(size));
        Page<UserFavRootEntity> favPage = userFavRootMapper.selectPage(pageParam,
                new LambdaQueryWrapper<UserFavRootEntity>()
                        .eq(UserFavRootEntity::getUserId, userId)
                        .orderByDesc(UserFavRootEntity::getCreateTime)
                        .orderByDesc(UserFavRootEntity::getId));

        List<Long> rootIds = selectFavIds(favPage.getRecords(), UserFavRootEntity::getRootId);
        if (rootIds.isEmpty()) {
            return PageVO.of(new ArrayList<>(), favPage.getTotal(),
                    favPage.getCurrent(), favPage.getSize());
        }

        Map<Long, RootEntity> rootMap = rootMapper.selectBatchIds(rootIds).stream()
                .collect(Collectors.toMap(RootEntity::getId, Function.identity()));
        return PageVO.of(sortByIds(rootIds, rootMap), favPage.getTotal(),
                favPage.getCurrent(), favPage.getSize());
    }

    /**
     * 提取收藏记录中的目标ID
     */
    private <T> List<Long> selectFavIds(List<T> favList, Function<T, Long> idGetter) {
        return favList.stream().map(idGetter).collect(Collectors.toList());
    }

    /**
     * 按收藏顺序（最近在前）重排结果，已删除的目标会被过滤
     */
    private <T> List<T> sortByIds(List<Long> ids, Map<Long, T> targetMap) {
        List<T> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            T target = targetMap.get(id);
            if (target != null) {
                result.add(target);
            }
        }
        return result;
    }
}