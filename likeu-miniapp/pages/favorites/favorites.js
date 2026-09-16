const request = require('../../utils/request');

Page({
  data: {
    activeTab: 0, // 0-单词 1-词根
    wordList: [],
    rootList: [],
    loading: true
  },

  onLoad(options) {
    if (options && options.tab === 'root') {
      this.setData({ activeTab: 1 });
    }
  },

  onShow() {
    // 每次进入/返回都刷新，保证取消收藏后数据同步
    this.loadAll();
  },

  loadAll() {
    if (!getApp().isLogin()) {
      this.setData({ loading: false, wordList: [], rootList: [] });
      return;
    }

    this.setData({ loading: true });
    Promise.all([
      request.get('/favorite/word/list').catch(() => []),
      request.get('/favorite/root/list').catch(() => [])
    ]).then(([wordList, rootList]) => {
      this.setData({
        wordList: wordList || [],
        rootList: rootList || [],
        loading: false
      });
    });
  },

  onTabChange(e) {
    this.setData({ activeTab: e.detail.index });
  },

  // 跳转到单词卡片
  goWord(e) {
    const wordId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/study/study?wordId=${wordId}` });
  },

  // 跳转到词根详情
  goRoot(e) {
    const rootId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/root-detail/root-detail?rootId=${rootId}` });
  },

  // 取消收藏单词
  removeWordFav(e) {
    const wordId = e.currentTarget.dataset.id;
    request.delQuery('/favorite/word', { wordId })
      .then(() => {
        wx.showToast({ title: '已取消收藏', icon: 'success' });
        this.loadAll();
      })
      .catch(() => {});
  },

  // 取消收藏词根
  removeRootFav(e) {
    const rootId = e.currentTarget.dataset.id;
    request.delQuery('/favorite/root', { rootId })
      .then(() => {
        wx.showToast({ title: '已取消收藏', icon: 'success' });
        this.loadAll();
      })
      .catch(() => {});
  }
});
