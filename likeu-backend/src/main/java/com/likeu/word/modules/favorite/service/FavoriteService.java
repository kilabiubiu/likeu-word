package com.likeu.word.modules.favorite.service;

import com.likeu.word.modules.root.entity.RootEntity;
import com.likeu.word.modules.word.entity.WordEntity;

import java.util.List;

/**
 * 收藏 Service 接口
 */
public interface FavoriteService {

    // ========== 单词收藏 ==========

    /**
     * 收藏单词
     */
    void addWordFav(Long userId, Long wordId);

    /**
     * 取消收藏单词
     */
    void removeWordFav(Long userId, Long wordId);

    /**
     * 是否已收藏单词
     */
    boolean isWordFav(Long userId, Long wordId);

    /**
     * 获取收藏单词数
     */
    int countWordFav(Long userId);

    /**
     * 获取收藏单词列表（按收藏时间倒序）
     */
    List<WordEntity> listFavWords(Long userId);

    // ========== 词根收藏 ==========

    /**
     * 收藏词根
     */
    void addRootFav(Long userId, Long rootId);

    /**
     * 取消收藏词根
     */
    void removeRootFav(Long userId, Long rootId);

    /**
     * 是否已收藏词根
     */
    boolean isRootFav(Long userId, Long rootId);

    /**
     * 获取收藏词根数
     */
    int countRootFav(Long userId);

    /**
     * 获取收藏词根列表（按收藏时间倒序）
     */
    List<RootEntity> listFavRoots(Long userId);
}