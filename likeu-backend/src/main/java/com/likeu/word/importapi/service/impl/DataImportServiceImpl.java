package com.likeu.word.importapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.likeu.word.importapi.dto.RootImportDTO;
import com.likeu.word.importapi.dto.WordImportDTO;
import com.likeu.word.importapi.dto.WordRootImportDTO;
import com.likeu.word.importapi.service.DataImportService;
import com.likeu.word.importapi.vo.ImportResultVO;
import com.likeu.word.mapper.RootMapper;
import com.likeu.word.mapper.WordMapper;
import com.likeu.word.mapper.WordRootMapper;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.entity.WordRootEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 基础数据批量导入实现
 *
 * <p>判存说明：{@code t_word}/{@code t_root}/{@code t_word_root} 在自然键上**没有**唯一索引，
 * 且 MyBatis-Plus 的逻辑删除会自动追加 {@code deleted = 0}，因此被软删的历史行查不到、
 * 「不存在则插入」也不会触发唯一键冲突；软删行与新行并存不影响业务读取。</p>
 *
 * <p>去重说明：批次内出现同一自然键时只保留最后一条，其余计入 {@code skipped}；
 * 判存用一次 {@code IN} 查询把相关行全部取回后在内存比对，避免逐条 select。</p>
 */
@Slf4j
@Service
public class DataImportServiceImpl implements DataImportService {

    private static final String KEY_SEP = "\u0000";

    @Resource
    private WordMapper wordMapper;

    @Resource
    private RootMapper rootMapper;

    @Resource
    private WordRootMapper wordRootMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importWords(List<WordImportDTO> items) {
        Map<String, WordImportDTO> unique = new LinkedHashMap<>();
        int skipped = 0;
        for (WordImportDTO item : items) {
            String key = wordKey(item.getWordBookId(), item.getWord());
            if (unique.put(key, item) != null) {
                skipped++;
            }
        }

        Set<Long> bookIds = new HashSet<>();
        Set<String> words = new HashSet<>();
        for (WordImportDTO item : unique.values()) {
            bookIds.add(item.getWordBookId());
            words.add(item.getWord().trim());
        }

        Map<String, WordEntity> existing = new HashMap<>();
        if (!bookIds.isEmpty()) {
            List<WordEntity> rows = wordMapper.selectList(new LambdaQueryWrapper<WordEntity>()
                    .in(WordEntity::getWordBookId, bookIds)
                    .in(WordEntity::getWord, words));
            for (WordEntity row : rows) {
                existing.putIfAbsent(wordKey(row.getWordBookId(), row.getWord()), row);
            }
        }

        int inserted = 0;
        int updated = 0;
        for (Map.Entry<String, WordImportDTO> entry : unique.entrySet()) {
            WordEntity exist = existing.get(entry.getKey());
            WordEntity entity = toWordEntity(entry.getValue());
            if (exist == null) {
                wordMapper.insert(entity);
                inserted++;
            } else {
                entity.setId(exist.getId());
                wordMapper.updateById(entity);
                updated++;
            }
        }

        log.info("导入单词完成: 请求={}, 新增={}, 更新={}, 批内重复={}", items.size(), inserted, updated, skipped);
        return ImportResultVO.of(inserted, updated, skipped);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importRoots(List<RootImportDTO> items) {
        Map<String, RootImportDTO> unique = new LinkedHashMap<>();
        int skipped = 0;
        for (RootImportDTO item : items) {
            if (unique.put(rootKey(item.getRoot()), item) != null) {
                skipped++;
            }
        }

        Set<String> roots = new HashSet<>();
        for (RootImportDTO item : unique.values()) {
            roots.add(item.getRoot().trim());
        }

        Map<String, RootEntity> existing = new HashMap<>();
        if (!roots.isEmpty()) {
            List<RootEntity> rows = rootMapper.selectList(new LambdaQueryWrapper<RootEntity>()
                    .in(RootEntity::getRoot, roots));
            for (RootEntity row : rows) {
                existing.putIfAbsent(rootKey(row.getRoot()), row);
            }
        }

        int inserted = 0;
        int updated = 0;
        for (Map.Entry<String, RootImportDTO> entry : unique.entrySet()) {
            RootEntity exist = existing.get(entry.getKey());
            RootImportDTO dto = entry.getValue();
            if (exist == null) {
                RootEntity entity = new RootEntity();
                entity.setType(dto.getType());
                entity.setRoot(dto.getRoot().trim());
                entity.setMeaning(dto.getMeaning());
                entity.setOrigin(dto.getOrigin());
                entity.setExample(dto.getExample());
                entity.setHot(0);
                rootMapper.insert(entity);
                inserted++;
            } else {
                // hot 是用户行为积累的热度，导入不覆盖
                RootEntity update = new RootEntity();
                update.setId(exist.getId());
                update.setType(dto.getType());
                update.setMeaning(dto.getMeaning());
                update.setOrigin(dto.getOrigin());
                update.setExample(dto.getExample());
                rootMapper.updateById(update);
                updated++;
            }
        }

        log.info("导入词根完成: 请求={}, 新增={}, 更新={}, 批内重复={}", items.size(), inserted, updated, skipped);
        return ImportResultVO.of(inserted, updated, skipped);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importWordRoots(List<WordRootImportDTO> items) {
        Map<String, WordRootImportDTO> unique = new LinkedHashMap<>();
        int skipped = 0;
        for (WordRootImportDTO item : items) {
            if (unique.put(wordRootKey(item.getWordId(), item.getRootId()), item) != null) {
                skipped++;
            }
        }

        Set<Long> wordIds = new HashSet<>();
        Set<Long> rootIds = new HashSet<>();
        for (WordRootImportDTO item : unique.values()) {
            wordIds.add(item.getWordId());
            rootIds.add(item.getRootId());
        }

        Map<String, WordRootEntity> existing = new HashMap<>();
        if (!wordIds.isEmpty()) {
            List<WordRootEntity> rows = wordRootMapper.selectList(new LambdaQueryWrapper<WordRootEntity>()
                    .in(WordRootEntity::getWordId, wordIds)
                    .in(WordRootEntity::getRootId, rootIds));
            for (WordRootEntity row : rows) {
                existing.putIfAbsent(wordRootKey(row.getWordId(), row.getRootId()), row);
            }
        }

        int inserted = 0;
        int updated = 0;
        for (Map.Entry<String, WordRootImportDTO> entry : unique.entrySet()) {
            WordRootImportDTO dto = entry.getValue();
            int position = dto.getPosition() != null ? dto.getPosition() : 0;
            WordRootEntity exist = existing.get(entry.getKey());
            if (exist == null) {
                WordRootEntity entity = new WordRootEntity();
                entity.setWordId(dto.getWordId());
                entity.setRootId(dto.getRootId());
                entity.setPosition(position);
                wordRootMapper.insert(entity);
                inserted++;
            } else {
                WordRootEntity update = new WordRootEntity();
                update.setId(exist.getId());
                update.setPosition(position);
                wordRootMapper.updateById(update);
                updated++;
            }
        }

        log.info("导入单词词根关联完成: 请求={}, 新增={}, 更新={}, 批内重复={}", items.size(), inserted, updated, skipped);
        return ImportResultVO.of(inserted, updated, skipped);
    }

    private WordEntity toWordEntity(WordImportDTO dto) {
        WordEntity entity = new WordEntity();
        entity.setWordBookId(dto.getWordBookId());
        entity.setWord(dto.getWord().trim());
        entity.setPhoneticUk(dto.getPhoneticUk());
        entity.setPhoneticUs(dto.getPhoneticUs());
        entity.setAudioUk(dto.getAudioUk());
        entity.setAudioUs(dto.getAudioUs());
        entity.setMeaningCn(dto.getMeaningCn());
        entity.setExampleEn(dto.getExampleEn());
        entity.setExampleCn(dto.getExampleCn());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return entity;
    }

    private String wordKey(Long wordBookId, String word) {
        return wordBookId + KEY_SEP + word.trim().toLowerCase(Locale.ROOT);
    }

    private String rootKey(String root) {
        return root.trim().toLowerCase(Locale.ROOT);
    }

    private String wordRootKey(Long wordId, Long rootId) {
        return wordId + KEY_SEP + rootId;
    }
}
