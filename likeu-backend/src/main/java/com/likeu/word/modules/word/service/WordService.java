package com.likeu.word.modules.word.service;

import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.vo.WordDetailVO;

import java.util.List;

/**
 * 单词 Service 接口
 */
public interface WordService {

    /**
     * 根据词书ID获取单词列表
     */
    List<WordEntity> getByBookId(Long bookId);

    /**
     * 根据ID获取单词详情（含词根拆解）
     */
    WordDetailVO getById(Long wordId);

    /**
     * 根据词根ID获取同源单词列表
     */
    List<WordEntity> getByRootId(Long rootId);
}