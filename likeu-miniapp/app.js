/**
 * LikeU 词根背单词小程序 - 入口
 */
const config = require('./utils/config');
const request = require('./utils/request');

App({
  globalData: {
    userInfo: null,
    token: '',
    userId: null,
    wordBookId: 1
  },

  onLaunch() {
    // callContainer 调用前必须 init 一次（全局一次即可）
    if (config.USE_CLOUD_CONTAINER && config.CLOUD_CONFIG_READY) {
      try {
        wx.cloud.init({ env: config.CLOUD_ENV_ID });
      } catch (e) {
        console.error('wx.cloud.init 失败，请检查云托管环境ID', e);
      }
    }

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
   * 登录（首次调用即注册），返回 Promise
   *
   * 云托管链路：请求经微信内网直达后端，后端从 X-WX-OPENID 请求头识别用户，
   * 因此不需要 wx.login，也不需要 code 换 session。
   * 本地调试链路：wx.login 取 code，交给后端走 code2Session。
   */
  login() {
    if (config.USE_CLOUD_CONTAINER) {
      return request.post('/user/login', {}).then(data => {
        this.applyLogin(data);
        return data;
      });
    }

    return new Promise((resolve, reject) => {
      wx.login({
        success: res => {
          if (!res.code) {
            wx.showToast({ title: '获取登录凭证失败', icon: 'none' });
            reject(res);
            return;
          }
          request.post('/user/login', { code: res.code })
            .then(data => {
              this.applyLogin(data);
              resolve(data);
            })
            .catch(reject);
        },
        fail: err => {
          wx.showToast({ title: '微信登录失败', icon: 'none' });
          reject(err);
        }
      });
    });
  },

  /**
   * 写入登录态
   */
  applyLogin(data) {
    this.setToken(data.token, data.userId, {
      nickName: data.nickname,
      avatarUrl: data.avatar
    });
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
