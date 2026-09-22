const request = require('../../utils/request');

const PAGE_SIZE = 20;

Page({
  data: {
    activeTab: 0, // 0-单词 1-词根
    wordList: [],
    wordTotal: 0,
    wordPage: 1,
    wordHasMore: false,
    rootList: [],
    rootTotal: 0,
    rootPage: 1,
    rootHasMore: false,
    loading: true,
    loadingMore: false
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

  // 触底加载当前 Tab 的下一页
  onReachBottom() {
    this.loadMore(this.data.activeTab === 0 ? 'word' : 'root');
  },

  loadAll() {
    if (!getApp().isLogin()) {
      // 一并复位 loadingMore，避免「加载更多」被切走时残留加载态
      this.setData({
        loading: false,
        loadingMore: false,
        wordList: [], wordTotal: 0, wordHasMore: false,
        rootList: [], rootTotal: 0, rootHasMore: false
      });
      return;
    }

    this.setData({ loading: true });
    Promise.all([
      this.fetchPage('word', 1),
      this.fetchPage('root', 1)
    ]).then(() => this.setData({ loading: false }));
  },

  loadMore(type) {
    if (this.data.loadingMore || !this.data[type + 'HasMore']) {
      return;
    }
    this.setData({ loadingMore: true });
    this.fetchPage(type, this.data[type + 'Page'] + 1)
      .then(() => this.setData({ loadingMore: false }));
  },

  /**
   * 拉取某一类的指定页
   * @param {string} type word | root
   * @param {number} page 页码，从 1 开始
   */
  fetchPage(type, page) {
    const url = type === 'word' ? '/favorite/word/list' : '/favorite/root/list';
    return request.get(url, { page, size: PAGE_SIZE })
      .then(res => {
        const records = (res && res.records) || [];
        const total = (res && res.total) || 0;
        const list = page === 1 ? records : this.data[type + 'List'].concat(records);
        const data = {};
        data[type + 'List'] = list;
        data[type + 'Total'] = total;
        data[type + 'Page'] = page;
        data[type + 'HasMore'] = list.length < total;
        this.setData(data);
      })
      .catch(() => {});
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
        this.fetchPage('word', 1);
      })
      .catch(() => {});
  },

  // 取消收藏词根
  removeRootFav(e) {
    const rootId = e.currentTarget.dataset.id;
    request.delQuery('/favorite/root', { rootId })
      .then(() => {
        wx.showToast({ title: '已取消收藏', icon: 'success' });
        this.fetchPage('root', 1);
      })
      .catch(() => {});
  }
});
