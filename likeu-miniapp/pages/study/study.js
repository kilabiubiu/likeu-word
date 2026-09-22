const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    wordList: [],
    currentIndex: 0,
    currentWord: null,
    isFlipped: false,
    isWordFav: false,
    // 进度条百分比，由 js 计算，避免列表为空时 wxml 里出现 Infinity
    progressPercent: 0,
    loading: true,
    // single: 从词根详情进入，只学指定单词；deck: 今日新词队列
    mode: 'deck',
    doneText: '恭喜，今日新词已学完！'
  },

  onLoad(options) {
    if (options && options.wordId) {
      this.setData({
        mode: 'single',
        doneText: '该单词已学完，可返回继续浏览'
      });
      this.loadSingleWord(options.wordId);
    } else {
      this.loadWords();
    }
  },

  // 加载指定单词（来自词根详情的同源词）
  loadSingleWord(wordId) {
    util.showLoading('加载单词...');
    request.get('/word/detail', { wordId })
      .then(word => {
        if (!word) {
          this.setData({ loading: false, wordList: [] });
          wx.showToast({ title: '单词不存在', icon: 'none' });
          return;
        }
        this.setData({
          wordList: [word],
          currentIndex: 0,
          currentWord: word,
          loading: false
        });
        this.updateProgress();
        this.loadWordFavStatus(word.id);
      })
      .catch(() => {
        this.setData({ loading: false, wordList: [] });
      })
      .finally(() => util.hideLoading());
  },

  // 加载新词
  loadWords() {
    util.showLoading('加载单词...');
    request.get('/study/new-words')
      .then(words => {
        this.setData({
          wordList: words || [],
          currentIndex: 0,
          currentWord: (words && words.length > 0) ? words[0] : null,
          loading: false
        });
        if (!words || words.length === 0) {
          wx.showToast({ title: '暂无新词', icon: 'none' });
        }
        this.updateProgress();
        this.loadWordFavStatus(this.data.currentWord && this.data.currentWord.id);
      })
      .catch(() => {
        this.setData({ loading: false });
      })
      .finally(() => util.hideLoading());
  },

  // 翻转卡片
  flipCard() {
    this.setData({ isFlipped: !this.data.isFlipped });
  },

  // 提交掌握度 (SM-2)
  submitQuality(e) {
    const quality = parseInt(e.currentTarget.dataset.quality);
    const word = this.data.currentWord;
    const wordId = word.id;

    if (!wordId) {
      wx.showToast({ title: '数据异常', icon: 'none' });
      return;
    }

    // 后端提交SM-2结果
    request.post('/study/submit', {
      wordId: wordId,
      quality: quality
    }).then(() => {
      // 切换到下一个单词
      this.nextWord();
    }).catch(() => {
      // 提交失败时停留在当前单词，允许用户重试（错误提示由 request 层统一给出）
    });
  },

  // 更新进度条百分比，列表为空时置 0
  updateProgress() {
    const { currentIndex, wordList } = this.data;
    const total = wordList.length;
    this.setData({
      progressPercent: total > 0 ? Math.round((currentIndex + 1) * 100 / total) : 0
    });
  },

  // 查询当前单词的收藏状态
  loadWordFavStatus(wordId) {
    if (!wordId || !getApp().isLogin()) {
      this.setData({ isWordFav: false });
      return;
    }
    request.get('/favorite/word/status', { wordId })
      .then(isFav => this.setData({ isWordFav: !!isFav }))
      .catch(() => this.setData({ isWordFav: false }));
  },

  // 收藏 / 取消收藏当前单词
  toggleWordFav() {
    if (!getApp().isLogin()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const { currentWord, isWordFav } = this.data;
    if (!currentWord || !currentWord.id) return;

    request[isWordFav ? 'delQuery' : 'postQuery']('/favorite/word', { wordId: currentWord.id })
      .then(() => {
        this.setData({ isWordFav: !isWordFav });
        wx.showToast({ title: isWordFav ? '已取消收藏' : '已收藏', icon: 'success' });
      })
      .catch(() => {});
  },

  // 下一个单词
  nextWord() {
    const { currentIndex, wordList } = this.data;
    const nextIndex = currentIndex + 1;

    // 先翻回正面，动画结束后再切换单词，避免背面闪现新释义
    this.setData({ isFlipped: false });

    setTimeout(() => {
      if (nextIndex < wordList.length) {
        this.setData({
          currentIndex: nextIndex,
          currentWord: wordList[nextIndex]
        });
        this.updateProgress();
        this.loadWordFavStatus(wordList[nextIndex].id);
      } else {
        this.setData({
          wordList: [],
          currentWord: null
        });
        this.updateProgress();
        wx.showToast({
          title: this.data.mode === 'single' ? '已学完' : '今日新词学完！',
          icon: 'success'
        });
      }
    }, 300);
  },

  // 跳转词根详情
  goRootDetail(e) {
    const root = e.currentTarget.dataset.root;
    wx.navigateTo({
      url: `/pages/root-detail/root-detail?rootId=${root.id}`
    });
  },

  // 返回首页
  backHome() {
    wx.navigateBack();
  }
});