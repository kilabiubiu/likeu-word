/**
 * 运行环境配置
 *
 * 真机预览与体验版无法访问 localhost，上线前需要把 release 换成正式域名。
 */
const BASE_URL_MAP = {
  // 微信开发者工具
  develop: 'http://localhost:8080/api',
  // 体验版
  trial: 'http://localhost:8080/api',
  // 正式版
  release: 'https://api.likeu.com/api'
};

/**
 * 按小程序 envVersion 选择后端地址，取不到时回退到 develop
 */
function resolveBaseUrl() {
  let envVersion = 'develop';
  if (typeof wx.getAccountInfoSync === 'function') {
    const accountInfo = wx.getAccountInfoSync();
    if (accountInfo && accountInfo.miniProgram && accountInfo.miniProgram.envVersion) {
      envVersion = accountInfo.miniProgram.envVersion;
    }
  }
  return BASE_URL_MAP[envVersion] || BASE_URL_MAP.develop;
}

module.exports = {
  BASE_URL: resolveBaseUrl()
};
