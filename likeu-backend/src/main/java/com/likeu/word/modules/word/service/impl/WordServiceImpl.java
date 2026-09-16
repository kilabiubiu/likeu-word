package com.likeu.word.modules.word.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.likeu.word.common.PageVO;
import com.likeu.word.mapper.RootMapper;
import com.likeu.word.mapper.WordMapper;
import com.likeu.word.mapper.WordRootMapper;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.entity.WordRootEntity;
import com.likeu.word.modules.word.service.WordService;
import com.likeu.word.modules.word.vo.WordDetailVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 单词 Service 实现
 */
@Service
public class WordServiceImpl implements WordService {

    @Resource
    private WordMapper wordMapper;

    @Resource
    private WordRootMapper wordRootMapper;

    @Resource
    private RootMapper rootMapper;

    @Override
    public PageVO<WordEntity> getByBookId(Long bookId, Integer page, Integer size) {
        Page<WordEntity> pageParam = new Page<>(
                PageVO.normalizePage(page), PageVO.normalizeSize(size));
        Page<WordEntity> result = wordMapper.selectPage(pageParam,
                new LambdaQueryWrapper<WordEntity>()
                        .eq(WordEntity::getWordBookId, bookId)
                        .orderByAsc(WordEntity::getSortOrder));
        return PageVO.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public WordDetailVO getById(Long wordId) {
        if (wordId == null) {
            return null;
        }
        List<WordDetailVO> list = getByIds(Collections.singletonList(wordId));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 批量获取单词详情（含词根拆解）
     * 固定 3 次查询，避免逐个单词查询造成的 N+1
     */
    @Override
    public List<WordDetailVO> getByIds(List<Long> wordIds) {
        if (wordIds == null || wordIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 批量查单词
        List<WordEntity> words = wordMapper.selectBatchIds(wordIds);
        if (words.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, WordEntity> wordMap = words.stream()
                .collect(Collectors.toMap(WordEntity::getId, w -> w));

        // 2. 批量查词根关联（按位置排序，保证拆解顺序）
        List<WordRootEntity> wordRoots = wordRootMapper.selectList(
                new LambdaQueryWrapper<WordRootEntity>()
                        .in(WordRootEntity::getWordId, wordIds)
                        .orderByAsc(WordRootEntity::getPosition));

        // 3. 批量查词根，避免逐个 selectById
        Set<Long> rootIds = wordRoots.stream()
                .map(WordRootEntity::getRootId)
                .collect(Collectors.toSet());
        Map<Long, RootEntity> rootMap = rootIds.isEmpty()
                ? Collections.emptyMap()
                : rootMapper.selectBatchIds(rootIds).stream()
                        .collect(Collectors.toMap(RootEntity::getId, r -> r));

        // 4. 按单词分组组装词根拆解
        Map<Long, List<WordDetailVO.RootChip>> chipMap = new HashMap<>();
        for (WordRootEntity wr : wordRoots) {
            RootEntity root = rootMap.get(wr.getRootId());
            if (root == null) {
                continue;
            }
            chipMap.computeIfAbsent(wr.getWordId(), k -> new ArrayList<>()).add(toChip(root));
        }

        // 5. 按传入顺序输出
        List<WordDetailVO> result = new ArrayList<>(wordIds.size());
        for (Long id : wordIds) {
            WordEntity word = wordMap.get(id);
            if (word == null) {
                continue;
            }
            result.add(WordDetailVO.from(word, chipMap.getOrDefault(id, new ArrayList<>())));
        }
        return result;
    }

    @Override
    public List<WordEntity> getByRootId(Long rootId) {
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

    private WordDetailVO.RootChip toChip(RootEntity root) {
        WordDetailVO.RootChip chip = new WordDetailVO.RootChip();
        chip.setId(root.getId());
        chip.setRoot(root.getRoot());
        chip.setMeaning(root.getMeaning());
        chip.setType(root.getType());
        return chip;
    }
}
