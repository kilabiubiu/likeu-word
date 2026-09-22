package com.likeu.word.modules.wordbook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.likeu.word.common.exception.BusinessException;
import com.likeu.word.common.util.CacheHelper;
import com.likeu.word.mapper.UserMapper;
import com.likeu.word.mapper.WordBookMapper;
import com.likeu.word.modules.user.entity.UserEntity;
import com.likeu.word.modules.wordbook.entity.WordBookEntity;
import com.likeu.word.modules.wordbook.service.WordBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 词书 Service 实现
 */
@Slf4j
@Service
public class WordBookServiceImpl implements WordBookService {

    private static final String CACHE_WORD_BOOK_LIST = "likeu:cache:wordbook:list";

    /** 词书列表返回条数上限：词书属极少量基础数据，不做分页，上限仅作脏数据防御 */
    private static final int MAX_BOOKS = 100;

    /** 词书列表缓存时长（分钟）：词书几乎不变，用长 TTL + 变更时主动失效 */
    @Value("${likeu.cache.wordbook-ttl-minutes:1440}")
    private long wordBookTtlMinutes;

    @Resource
    private WordBookMapper wordBookMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CacheHelper cacheHelper;

    @Override
    public List<WordBookEntity> getList() {
        return cacheHelper.getList(CACHE_WORD_BOOK_LIST, WordBookEntity.class,
                wordBookTtlMinutes * 60, this::queryAllBooks);
    }

    private List<WordBookEntity> queryAllBooks() {
        return wordBookMapper.selectList(
                new LambdaQueryWrapper<WordBookEntity>()
                        .orderByAsc(WordBookEntity::getSortOrder)
                        .last("LIMIT " + MAX_BOOKS));
    }

    @Override
    public WordBookEntity getCurrent(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getWordBookId() == null) {
            return null;
        }
        return wordBookMapper.selectById(user.getWordBookId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchBook(Long userId, Long bookId) {
        // 检查词书是否存在
        WordBookEntity book = wordBookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("词书不存在");
        }
        // 更新用户选择的词书
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setWordBookId(bookId);
        userMapper.updateById(user);
        log.info("用户切换词书: userId={}, bookId={}, bookName={}", userId, bookId, book.getName());
    }
}