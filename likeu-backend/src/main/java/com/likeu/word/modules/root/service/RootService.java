package com.likeu.word.modules.root.service;

import com.likeu.word.common.PageVO;
import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.word.entity.WordEntity;

import java.util.List;

/**
 * 词根 Service 接口
 */
public interface RootService {

    /**
     * 列表查询（支持类型筛选和关键词搜索）
     */
    PageVO<RootEntity> list(Integer type, String keyword, Integer page, Integer size);

    /**
     * 获取词根详情
     */
    RootEntity detail(Long rootId);

    /**
     * 获取词根同源单词列表
     */
    List<WordEntity> getWords(Long rootId);

    /**
     * 增加热度（被查看一次+1）
     */
    void increaseHot(Long rootId);
}