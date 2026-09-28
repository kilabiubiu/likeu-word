/**
 * 运行环境配置
 *
 * 真机预览与体验版无法访问 localhost，上线前需要把 release 换成正式域名。
 * 云托管部署后，微信会分配一个默认域名（如 likeu-word-xxxxx.service.tcloudbase.com），
 * 可在云托管控制台「域名管理」中查看或绑定自定义域名。
 */
const BASE_URL_MAP = {
  // 微信开发者工具
  develop: 'http://localhost:8080/api',
  // 体验版：与 develop 共用 localhost；真机调试时需后端可外网访问
  trial: 'http://localhost:8080/api',
  // 云托管生产环境 —— 替换成云托管分配的服务域名，或自己绑定的域名
  // 微信要求后端域名必须是 HTTPS 合法证书
  // 示例：'https://likeu-word-xxxxx.service.tcloudbase.com/api'
  release: 'https://YOUR-CLOUDRUN-DOMAIN/api'
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
