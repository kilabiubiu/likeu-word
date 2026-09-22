package com.likeu.word.importapi.service;

import com.likeu.word.importapi.dto.RootImportDTO;
import com.likeu.word.importapi.dto.WordImportDTO;
import com.likeu.word.importapi.dto.WordRootImportDTO;
import com.likeu.word.importapi.vo.ImportResultVO;

import java.util.List;

/**
 * 基础数据批量导入 Service
 *
 * <p>统一按业务自然键判存：存在则更新、不存在则插入，整批在同一事务内完成。</p>
 */
public interface DataImportService {

    /**
     * 批量导入单词，按 {@code (word_book_id, word)} 判存
     */
    ImportResultVO importWords(List<WordImportDTO> items);

    /**
     * 批量导入词根词缀，按 {@code root} 文本判存
     */
    ImportResultVO importRoots(List<RootImportDTO> items);

    /**
     * 批量导入单词-词根关联，按 {@code (word_id, root_id)} 判存
     */
    ImportResultVO importWordRoots(List<WordRootImportDTO> items);
}
