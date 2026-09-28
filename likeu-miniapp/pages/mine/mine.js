const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    userInfo: null,
    totalStats: {
      totalWords: 0,
      masteredWords: 0,
      favWords: 0,
      favRoots: 0
    }
  },

  onShow() {
    const app = getApp();
    this.setData({
      userInfo: app.globalData.userInfo
    });

    if (app.isLogin()) {
      this.loadStats();
    }
  },

  // 登录：兼顾云托管（openid 直传）与本地调试（code2Session），统一走 app.login()
  handleLogin() {
    const app = getApp();
    util.showLoading('登录中...');
    app.login()
      .then(() => {
        this.setData({
          userInfo: app.globalData.userInfo
        });
        this.loadStats();
      })
      .catch(() => {})
      .finally(() => util.hideLoading());
  },

  loadStats() {
    request.get('/user/stats')
      .then(data => {
        this.setData({ totalStats: data });
      })
      .catch(() => {});
  },

  goFavWords() {
    if (!getApp().isLogin()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/favorites/favorites?tab=word' });
  },

  goFavRoots() {
    if (!getApp().isLogin()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/favorites/favorites?tab=root' });
  },

  resetProgress() {
    if (!getApp().isLogin()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '确认重置',
      content: '重置后所有学习记录将被清除，确定继续？',
      success: (res) => {
        if (res.confirm) {
          util.showLoading('重置中...');
          request.post('/user/reset')
            .then(() => {
              wx.showToast({ title: '重置成功', icon: 'success' });
              this.loadStats();
            })
            .catch(() => {})
            .finally(() => util.hideLoading());
        }
      }
    });
  },

  logout() {
    const app = getApp();
    app.clearLogin();
    this.setData({
      userInfo: null,
      totalStats: {
        totalWords: 0,
        masteredWords: 0,
        favWords: 0,
        favRoots: 0
      }
    });
    wx.showToast({ title: '已退出登录', icon: 'none' });
  }
});