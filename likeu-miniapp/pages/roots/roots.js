const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    rootList: [],
    keyword: '',
    filterType: 0, // 0-全部 1-前缀 2-词根 3-后缀
    loading: true
  },

  onLoad() {
    this.loadRoots();
  },

  onShow() {
    // 从详情页返回时刷新热度
    this.loadRoots();
  },

  loadRoots() {
    this.setData({ loading: true });
    const params = {};
    if (this.data.filterType > 0) {
      params.type = this.data.filterType;
    }
    if (this.data.keyword) {
      params.keyword = this.data.keyword;
    }

    request.get('/root/list', params)
      .then(data => {
        this.setData({
          rootList: data || [],
          loading: false
        });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  switchFilter(e) {
    const type = parseInt(e.currentTarget.dataset.type);
    this.setData({ filterType: type }, () => {
      this.loadRoots();
    });
  },

  onSearch(e) {
    this.setData({ keyword: e.detail }, () => {
      this.loadRoots();
    });
  },

  onClear() {
    this.setData({ keyword: '' }, () => {
      this.loadRoots();
    });
  },

  goDetail(e) {
    const rootId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/root-detail/root-detail?rootId=${rootId}`
    });
  }
});