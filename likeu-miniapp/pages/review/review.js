const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    wordList: [],
    currentIndex: 0,
    currentWord: null,
    isFlipped: false,
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
      this.nextWord();
    });
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
      } else {
        this.setData({
          wordList: [],
          currentWord: null
        });
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