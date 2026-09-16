package com.likeu.word.modules.root.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.stream.Collectors;

/**
 * 词根 Service 实现
 */
@Slf4j
@Service
public class RootServiceImpl implements RootService {

    @Resource
    private RootMapper rootMapper;

    @Resource
    private WordRootMapper wordRootMapper;

    @Resource
    private WordMapper wordMapper;

    @Resource
    private FavoriteService favoriteService;

    @Override
    public List<RootEntity> list(Integer type, String keyword) {
        LambdaQueryWrapper<RootEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(RootEntity::getHot);

        if (type != null && type > 0) {
            wrapper.eq(RootEntity::getType, type);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(RootEntity::getRoot, keyword.trim());
        }

        return rootMapper.selectList(wrapper);
    }

    @Override
    public RootEntity detail(Long rootId) {
        return rootMapper.selectById(rootId);
    }

    @Override
    public List<WordEntity> getWords(Long rootId) {
        List<WordRootEntity> wordRoots = wordRootMapper.selectList(
                new LambdaQueryWrapper<WordRootEntity>()
                        .eq(WordRootEntity::getRootId, rootId));

        if (wordRoots.isEmpty()) return new ArrayList<>();

        List<Long> wordIds = wordRoots.stream()
                .map(WordRootEntity::getWordId)
                .collect(Collectors.toList());

        return wordMapper.selectList(
                new LambdaQueryWrapper<WordEntity>()
                        .in(WordEntity::getId, wordIds));
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
}