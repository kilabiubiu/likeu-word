/**
 * LikeU 词根背单词小程序 - 入口
 */
App({
  globalData: {
    userInfo: null,
    token: '',
    userId: null,
    wordBookId: 1
  },

  onLaunch() {
    // 读取本地缓存登录态
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    const userId = wx.getStorageSync('userId');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo || null;
      this.globalData.userId = userId || null;
      this.globalData.wordBookId = wx.getStorageSync('wordBookId') || 1;
    }
  },

  /**
   * 设置token
   */
  setToken(token, userId, userInfo) {
    this.globalData.token = token;
    this.globalData.userId = userId;
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('token', token);
    wx.setStorageSync('userId', userId);
    wx.setStorageSync('userInfo', userInfo);
  },

  /**
   * 清除登录状态
   */
  clearLogin() {
    this.globalData.token = '';
    this.globalData.userId = null;
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userId');
    wx.removeStorageSync('userInfo');
  },

  /**
   * 检查是否已登录
   */
  isLogin() {
    return !!this.globalData.token;
  }
});