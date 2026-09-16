/**
 * 网络请求封装（自动携带token）
 */
const BASE_URL = 'http://localhost:8080/api';

/**
 * 将参数对象拼成 query string
 */
function toQuery(params) {
  if (!params) return '';
  const parts = [];
  Object.keys(params).forEach(key => {
    const value = params[key];
    if (value === undefined || value === null || value === '') return;
    parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
  });
  return parts.length ? '?' + parts.join('&') : '';
}

/**
 * 发起HTTP请求
 */
function request(url, method, data, options = {}) {
  const app = getApp();
  const token = app.globalData.token;

  // asQuery: 把参数拼到 URL 上，用于后端用 @RequestParam 接收的 POST/DELETE 接口
  const finalUrl = options.asQuery ? BASE_URL + url + toQuery(data) : BASE_URL + url;
  const finalData = options.asQuery ? undefined : data;

  return new Promise((resolve, reject) => {
    wx.request({
      url: finalUrl,
      method: method,
      data: finalData,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      timeout: options.timeout || 10000,
      success(res) {
        if (res.statusCode === 401) {
          // token过期，跳转登录（由各页面处理）
          wx.showToast({ title: '登录已过期', icon: 'none' });
          app.clearLogin();
          // 触发全局登录事件
          // index 是 tabBar 页面，必须用 switchTab
          wx.switchTab({ url: '/pages/index/index' });
          reject({ code: 401, msg: '登录已过期' });
          return;
        }

        if (res.statusCode >= 200 && res.statusCode < 300) {
          const result = res.data;
          if (result.code === 200) {
            resolve(result.data);
          } else {
            wx.showToast({ title: result.message || '请求失败', icon: 'none' });
            reject(result);
          }
        } else {
          reject({ code: res.statusCode, msg: '网络错误' });
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查网络', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = {
  get(url, params, options) {
    return request(url, 'GET', params, options);
  },
  post(url, data, options) {
    return request(url, 'POST', data, options);
  },
  put(url, data, options) {
    return request(url, 'PUT', data, options);
  },
  del(url, data, options) {
    return request(url, 'DELETE', data, options);
  },
  // 后端用 @RequestParam 接收参数的 POST / DELETE 接口
  postQuery(url, params, options) {
    return request(url, 'POST', params, Object.assign({}, options, { asQuery: true }));
  },
  delQuery(url, params, options) {
    return request(url, 'DELETE', params, Object.assign({}, options, { asQuery: true }));
  },
  BASE_URL
};