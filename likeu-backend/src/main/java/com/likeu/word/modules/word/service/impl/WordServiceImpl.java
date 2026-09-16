package com.likeu.word.modules.word.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.Comparator;
import java.util.List;
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
    public List<WordEntity> getByBookId(Long bookId) {
        return wordMapper.selectList(
                new LambdaQueryWrapper<WordEntity>()
                        .eq(WordEntity::getWordBookId, bookId)
                        .orderByAsc(WordEntity::getSortOrder));
    }

    @Override
    public WordDetailVO getById(Long wordId) {
        WordEntity word = wordMapper.selectById(wordId);
        if (word == null) return null;

        // 查询词根关联
        List<WordRootEntity> wordRoots = wordRootMapper.selectList(
                new LambdaQueryWrapper<WordRootEntity>()
                        .eq(WordRootEntity::getWordId, wordId)
                        .orderByAsc(WordRootEntity::getPosition));

        List<WordDetailVO.RootChip> chips = new ArrayList<>();
        for (WordRootEntity wr : wordRoots) {
            RootEntity root = rootMapper.selectById(wr.getRootId());
            if (root != null) {
                WordDetailVO.RootChip chip = new WordDetailVO.RootChip();
                chip.setId(root.getId());
                chip.setRoot(root.getRoot());
                chip.setMeaning(root.getMeaning());
                chip.setType(root.getType());
                chips.add(chip);
            }
        }

        return WordDetailVO.from(word, chips);
    }

    @Override
    public List<WordEntity> getByRootId(Long rootId) {
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
}