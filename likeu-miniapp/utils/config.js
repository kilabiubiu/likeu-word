/**
 * 运行环境配置
 *
 * 后端部署在「微信云托管」上，小程序按运行环境走两条调用通道：
 *
 *  1. 微信开发者工具（envVersion = develop）
 *     用 wx.request 直连本机后端，方便本地断点调试。
 *
 *  2. 体验版 / 正式版（envVersion = trial / release）
 *     用 wx.cloud.callContainer 走微信内网直连云托管服务：
 *       - 不需要域名、不需要 ICP 备案、不需要在小程序后台配置「服务器域名」
 *       - 后端能从请求头直接拿到 X-WX-OPENID，因此无需 wx.login 换 code
 *       - 请求与后端走内网，不计公网流量，且只有本小程序能访问
 *
 * ⚠️ 不要用云托管的「默认公网域名」给正式版调用：官方明确默认公网域名性能受限、
 * 仅用于接口测试，且小程序后台无法把它配置为 request 合法域名。
 */
const CLOUD_ENV_ID = 'REPLACE_WITH_CLOUDRUN_ENV_ID'; // 微信云托管环境 ID，形如 prod-xxxxxxxx
const CLOUD_SERVICE_NAME = 'likeu-word'; // 云托管「服务名称」，见控制台服务列表
const CLOUD_API_PREFIX = '/api'; // 后端 context-path，见 application.yml 的 server.servlet.context-path

/** 想在开发者工具里直接验证云托管通道时，把它改成 true（需先在工具里开通/关联该云环境） */
const FORCE_CLOUD_IN_DEVTOOLS = false;

/** 本地调试用的后端地址（develop 环境生效） */
const LOCAL_BASE_URL = 'http://localhost:8080/api';

/**
 * 取小程序当前运行环境：develop / trial / release
 */
function resolveEnvVersion() {
  try {
    if (typeof wx.getAccountInfoSync === 'function') {
      const accountInfo = wx.getAccountInfoSync();
      if (accountInfo && accountInfo.miniProgram && accountInfo.miniProgram.envVersion) {
        return accountInfo.miniProgram.envVersion;
      }
    }
  } catch (e) {
    // 取不到就按开发者工具处理
  }
  return 'develop';
}

const ENV_VERSION = resolveEnvVersion();

/** 是否走云托管 callContainer：除开发者工具外一律走云托管（可用开关强制在工具里验证云通道） */
const USE_CLOUD_CONTAINER = FORCE_CLOUD_IN_DEVTOOLS || ENV_VERSION !== 'develop';

/** 云托管配置是否已填写，未填写时请求层会给出明确提示而不是静默失败 */
const CLOUD_CONFIG_READY = !!(CLOUD_ENV_ID && CLOUD_ENV_ID.indexOf('REPLACE_WITH') !== 0);

module.exports = {
  ENV_VERSION,
  USE_CLOUD_CONTAINER,
  CLOUD_ENV_ID,
  CLOUD_SERVICE_NAME,
  CLOUD_API_PREFIX,
  LOCAL_BASE_URL,
  CLOUD_CONFIG_READY
};
