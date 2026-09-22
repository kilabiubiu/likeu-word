const request = require('../../utils/request');
const util = require('../../utils/util');

const PAGE_SIZE = 20;

Page({
  data: {
    rootList: [],
    keyword: '',
    filterType: 0, // 0-全部 1-前缀 2-词根 3-后缀
    loading: true,
    loadingMore: false,
    hasMore: true,
    page: 1,
    // 未登录时用于区分空态文案
    needLogin: false
  },

  onLoad() {
    this.loadRoots(true);
  },

  onShow() {
    // 从详情页返回时刷新热度
    this.loadRoots(true);
  },

  // 触底加载下一页
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.loadRoots(false);
    }
  },

  /**
   * @param {boolean} reset 是否从第一页重新加载
   */
  loadRoots(reset) {
    // 未登录时不请求需要鉴权的接口，避免触发无意义的 401
    if (!getApp().isLogin()) {
      this.setData({
        rootList: [], loading: false, loadingMore: false, hasMore: false, needLogin: true
      });
      return;
    }

    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true, needLogin: false } : { loadingMore: true });

    const params = { page, size: PAGE_SIZE };
    if (this.data.filterType > 0) {
      params.type = this.data.filterType;
    }
    if (this.data.keyword) {
      params.keyword = this.data.keyword;
    }

    request.get('/root/list', params)
      .then(res => {
        const records = (res && res.records) || [];
        const list = reset ? records : this.data.rootList.concat(records);
        this.setData({
          rootList: list,
          page,
          hasMore: list.length < ((res && res.total) || 0),
          loading: false,
          loadingMore: false
        });
      })
      .catch(() => {
        this.setData({ loading: false, loadingMore: false });
      });
  },

  switchFilter(e) {
    const type = parseInt(e.currentTarget.dataset.type);
    this.setData({ filterType: type }, () => {
      this.loadRoots(true);
    });
  },

  onSearch(e) {
    this.setData({ keyword: e.detail }, () => {
      this.loadRoots(true);
    });
  },

  onClear() {
    this.setData({ keyword: '' }, () => {
      this.loadRoots(true);
    });
  },

  goDetail(e) {
    const rootId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/root-detail/root-detail?rootId=${rootId}`
    });
  }
});
