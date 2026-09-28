/**
 * 网络请求封装
 *
 * 两条通道共用同一套 API 与响应处理：
 *  - develop（开发者工具）：wx.request 直连本机后端
 *  - trial / release：wx.cloud.callContainer 走微信内网调用云托管服务
 *    （无需域名/备案/服务器域名配置，后端从 X-WX-OPENID 请求头识别用户）
 *
 * 两者都保留 Authorization 头：本地调试依赖 JWT，云托管链路下该头只是无害的兼容信息。
 */
const config = require('./config');

/**
 * 401 处理去重标志：多个请求同时过期时，只提示并跳转一次
 */
let handling401 = false;

/**
 * 统一处理 401
 *
 * @param app 应用实例
 * @param hadToken 本次请求是否携带了 token
 */
function handle401(app, hadToken) {
  // 没有 token 说明只是尚未登录，不属于「登录已过期」，不打扰用户（页面自己决定是否引导登录）
  if (!hadToken || handling401) {
    return;
  }
  handling401 = true;
  wx.showToast({ title: '登录已过期', icon: 'none' });
  app.clearLogin();
  // index 是 tabBar 页面，必须用 switchTab
  wx.switchTab({ url: '/pages/index/index' });
  setTimeout(() => {
    handling401 = false;
  }, 1500);
}

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
 * 统一的响应处理：解析 Result{code,message,data}
 */
function handleResponse(res, app, hadToken, resolve, reject) {
  if (res.statusCode === 401) {
    handle401(app, hadToken);
    reject({ code: 401, msg: '未登录或登录已过期' });
    return;
  }

  if (res.statusCode >= 200 && res.statusCode < 300) {
    const result = res.data;
    if (result && result.code === 200) {
      resolve(result.data);
    } else {
      wx.showToast({ title: (result && result.message) || '请求失败', icon: 'none' });
      reject(result);
    }
    return;
  }

  reject({ code: res.statusCode, msg: '网络错误' });
}

/**
 * 发起请求
 */
function request(url, method, data, options = {}) {
  const app = getApp();
  const token = app.globalData.token;
  const hadToken = !!token;

  const header = {
    'Content-Type': 'application/json',
    'Authorization': token ? 'Bearer ' + token : ''
  };

  // asQuery: 把参数拼到 URL 上，用于后端用 @RequestParam 接收的 POST/DELETE 接口
  const asQuery = !!options.asQuery;
  const query = asQuery ? toQuery(data) : '';
  const timeout = options.timeout || 10000;

  return new Promise((resolve, reject) => {
    if (config.USE_CLOUD_CONTAINER) {
      if (!config.CLOUD_CONFIG_READY) {
        wx.showToast({ title: '云托管环境ID未配置', icon: 'none' });
        reject({ code: -1, msg: 'CLOUD_ENV_ID 未配置，请见 utils/config.js' });
        return;
      }
      wx.cloud.callContainer({
        config: { env: config.CLOUD_ENV_ID },
        // path 必须是「根目录 + context-path + 业务路径」，云托管按原样转发给容器
        path: config.CLOUD_API_PREFIX + url + query,
        method: method,
        header: Object.assign({ 'X-WX-SERVICE': config.CLOUD_SERVICE_NAME }, header),
        data: asQuery ? undefined : data,
        timeout: timeout,
        success(res) {
          handleResponse(res, app, hadToken, resolve, reject);
        },
        fail(err) {
          wx.showToast({ title: '服务暂不可用，请稍后重试', icon: 'none' });
          reject(err);
        }
      });
      return;
    }

    wx.request({
      url: config.LOCAL_BASE_URL + url + query,
      method: method,
      data: asQuery ? undefined : data,
      header: header,
      timeout: timeout,
      success(res) {
        handleResponse(res, app, hadToken, resolve, reject);
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
  }
};
