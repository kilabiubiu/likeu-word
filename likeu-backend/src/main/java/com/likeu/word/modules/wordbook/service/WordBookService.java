package com.likeu.word.modules.wordbook.service;

import com.likeu.word.modules.wordbook.entity.WordBookEntity;

import java.util.List;

/**
 * 词书 Service 接口
 */
public interface WordBookService {

    /**
     * 获取所有词书列表
     */
    List<WordBookEntity> getList();

    /**
     * 获取当前用户选中的词书
     */
    WordBookEntity getCurrent(Long userId);

    /**
     * 切换词书
     */
    void switchBook(Long userId, Long bookId);
}