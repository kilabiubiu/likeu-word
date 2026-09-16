package com.likeu.word.modules.word.service;

import com.likeu.word.common.PageVO;
import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.vo.WordDetailVO;

import java.util.List;

/**
 * 单词 Service 接口
 */
public interface WordService {

    /**
     * 根据词书ID分页获取单词列表
     */
    PageVO<WordEntity> getByBookId(Long bookId, Integer page, Integer size);

    /**
     * 根据ID获取单词详情（含词根拆解）
     */
    WordDetailVO getById(Long wordId);

    /**
     * 批量获取单词详情（含词根拆解），固定 3 次查询
     */
    List<WordDetailVO> getByIds(List<Long> wordIds);

    /**
     * 根据词根ID获取同源单词列表
     */
    List<WordEntity> getByRootId(Long rootId);
}