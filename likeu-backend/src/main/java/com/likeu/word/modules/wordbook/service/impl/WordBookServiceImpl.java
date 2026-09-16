package com.likeu.word.modules.wordbook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.likeu.word.common.exception.BusinessException;
import com.likeu.word.mapper.UserMapper;
import com.likeu.word.mapper.WordBookMapper;
import com.likeu.word.modules.user.entity.UserEntity;
import com.likeu.word.modules.wordbook.entity.WordBookEntity;
import com.likeu.word.modules.wordbook.service.WordBookService;
import lombok.extern.slf4j.Slf4j;
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

    @Resource
    private WordBookMapper wordBookMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public List<WordBookEntity> getList() {
        return wordBookMapper.selectList(
                new LambdaQueryWrapper<WordBookEntity>()
                        .orderByAsc(WordBookEntity::getSortOrder));
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