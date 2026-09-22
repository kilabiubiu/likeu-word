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
    loading: true
  },

  onLoad() {
    this.loadReviewWords();
  },

  // 加载待复习单词
  loadReviewWords() {
    util.showLoading('加载复习...');
    request.get('/study/review-words')
      .then(words => {
        this.setData({
          wordList: words || [],
          currentIndex: 0,
          currentWord: (words && words.length > 0) ? words[0] : null,
          loading: false
        });
        this.updateProgress();
        this.loadWordFavStatus(this.data.currentWord && this.data.currentWord.id);
      })
      .catch(() => {
        this.setData({ loading: false });
      })
      .finally(() => util.hideLoading());
  },

  flipCard() {
    this.setData({ isFlipped: !this.data.isFlipped });
  },

  submitQuality(e) {
    const quality = parseInt(e.currentTarget.dataset.quality);
    const word = this.data.currentWord;
    const wordId = word.id;

    if (!wordId) return;

    request.post('/study/submit', {
      wordId: wordId,
      quality: quality
    }).then(() => {
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

  nextWord() {
    const { currentIndex, wordList } = this.data;
    const nextIndex = currentIndex + 1;

    // 先翻回正面，动画结束后再切换单词
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
        wx.showToast({ title: '复习完成！', icon: 'success' });
      }
    }, 300);
  },

  goRootDetail(e) {
    const root = e.currentTarget.dataset.root;
    wx.navigateTo({
      url: `/pages/root-detail/root-detail?rootId=${root.id}`
    });
  },

  backHome() {
    wx.navigateBack();
  }
});