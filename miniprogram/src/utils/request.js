const DEV_BASE_URL = 'http://localhost:8090/api';
const PROD_BASE_URL = 'https://api.xhyscrapcar.com/api';

let BASE_URL = DEV_BASE_URL;

// #ifdef MP-WEIXIN
// 微信小程序环境：优先使用微信官方API判断环境
const accountInfo = uni.getAccountInfoSync();
// envVersion: 'develop' (开发版), 'trial' (体验版), 'release' (正式版)
const envVersion = accountInfo.miniProgram ? accountInfo.miniProgram.envVersion : 'develop';

console.log('WeChat Env:', envVersion);

if (envVersion === 'trial' || envVersion === 'release') {
  // 体验版和正式版，强制连接生产环境，无视构建模式
  BASE_URL = PROD_BASE_URL;
} else {
  // 开发版 (develop)
  // 如果是真机调试（非模拟器），localhost 也是连不上的，建议手动改为局域网IP
  // 但为了不影响电脑模拟器调试，默认保持 localhost
  BASE_URL = DEV_BASE_URL;
}
// #endif

// #ifndef MP-WEIXIN
// 非微信小程序环境（如H5/App），使用构建模式判断
if (process.env.NODE_ENV === 'production') {
  BASE_URL = PROD_BASE_URL;
}
// #endif

console.log('Final API URL:', BASE_URL);

export const API_BASE_URL = BASE_URL;

const request = (options) => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Authorization': uni.getStorageSync('token') ? 'Bearer ' + uni.getStorageSync('token') : '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data);
        } else if (res.statusCode === 401) {
          uni.showToast({ title: '请先登录', icon: 'none' });
          uni.navigateTo({ url: '/pages/login/login' });
          reject(res);
        } else {
          uni.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          reject(res);
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络请求失败', icon: 'none' });
        reject(err);
      }
    });
  });
};

export default request;
