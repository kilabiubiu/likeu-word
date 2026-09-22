const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    userInfo: null,
    currentBook: null,
    todayStats: {
      newCount: 0,
      reviewCount: 0,
      dueCount: 0,
      masteredCount: 0,
      progress: 0
    },
    dailyNewLimit: 20,
    books: []
  },

  onShow() {
    const app = getApp();
    const userInfo = app.globalData.userInfo;

    // 检查登录状态变化
    if (userInfo !== this.data.userInfo) {
      this.setData({ userInfo });
    }

    this.loadStats();
    this.loadBookInfo();
  },

  // 微信登录
  handleLogin() {
    const app = getApp();
    wx.login({
      success: (res) => {
        if (res.code) {
          util.showLoading('登录中...');
          request.post('/user/login', { code: res.code })
            .then(data => {
              app.setToken(data.token, data.userId, {
                nickName: data.nickname,
                avatarUrl: data.avatar
              });
              this.setData({
                userInfo: app.globalData.userInfo
              });
              // 重新加载数据
              this.loadStats();
              this.loadBookInfo();
            })
            .catch(() => {})
            .finally(() => util.hideLoading());
        }
      }
    });
  },

  // 加载今日统计
  loadStats() {
    if (!getApp().isLogin()) return;

    request.get('/study/stats/today')
      .then(data => this.setData({
        todayStats: data,
        // 每日新词上限由后端返回，避免前端写死与后端不一致
        dailyNewLimit: data.dailyNewLimit || this.data.dailyNewLimit
      }))
      .catch(() => {});
  },

  // 加载词书信息
  loadBookInfo() {
    // 未登录时不请求这些需要鉴权的接口，避免触发 401 提示
    if (!getApp().isLogin()) return;

    request.get('/word-book/current')
      .then(data => this.setData({ currentBook: data }))
      .catch(() => {});

    // 获取词书列表供切换
    request.get('/word-book/list')
      .then(data => this.setData({ books: data }))
      .catch(() => {});
  },

  // 切换词书
  switchBook() {
    const { books } = this.data;
    if (!books || books.length === 0) return;

    wx.showActionSheet({
      itemList: books.map(b => b.name),
      success: (res) => {
        const book = books[res.tapIndex];
        request.postQuery('/word-book/switch', { bookId: book.id })
          .then(() => {
            this.setData({ currentBook: book });
            getApp().globalData.wordBookId = book.id;
            wx.setStorageSync('wordBookId', book.id);
            wx.showToast({ title: '已切换', icon: 'success' });
          })
          .catch(() => {});
      }
    });
  },

  // 开始学习新词
  startLearning() {
    if (!getApp().isLogin()) {
      this.handleLogin();
      return;
    }
    if (!this.data.currentBook) {
      wx.showToast({ title: '请先选择词书', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/study/study' });
  },

  // 开始复习
  startReview() {
    if (!getApp().isLogin()) {
      this.handleLogin();
      return;
    }
    if (this.data.todayStats.dueCount <= 0) {
      wx.showToast({ title: '今日暂无待复习单词', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/review/review' });
  }
});