const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    root: null,
    wordList: [],
    isFav: false,
    loading: true
  },

  onLoad(options) {
    if (options.rootId) {
      this.setData({ rootId: options.rootId });
      this.loadRootDetail(options.rootId);
    }
  },

  loadRootDetail(rootId) {
    util.showLoading('加载中...');

    // 未登录时跳过收藏状态请求，否则会触发 401 的全局提示与跳转
    const favTask = getApp().isLogin()
      ? request.get('/favorite/root/status', { rootId }).catch(() => false)
      : Promise.resolve(false);

    Promise.all([
      request.get('/root/detail', { rootId }),
      request.get('/root/words', { rootId }),
      favTask
    ]).then(([root, words, favStatus]) => {
      this.setData({
        root,
        wordList: words || [],
        isFav: !!favStatus,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false });
    }).finally(() => util.hideLoading());
  },

  toggleFav() {
    if (!getApp().isLogin()) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const { rootId, isFav } = this.data;

    request[isFav ? 'delQuery' : 'postQuery']('/favorite/root', { rootId })
      .then(() => {
        this.setData({ isFav: !isFav });
        wx.showToast({
          title: isFav ? '已取消收藏' : '已收藏',
          icon: 'success'
        });
      })
      .catch(() => {});
  },

  goStudy(e) {
    const wordId = e.currentTarget.dataset.wordid;
    // 跳转到学习页并指定单词
    wx.navigateTo({
      url: `/pages/study/study?wordId=${wordId}`
    });
  }
});