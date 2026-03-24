const DEV_BASE_URL = 'http://localhost:8090/api';
const PROD_BASE_URL = 'https://api.xhyscrapcar.com/api';

let BASE_URL = DEV_BASE_URL;

// #ifdef MP-WEIXIN
const accountInfo = uni.getAccountInfoSync ? uni.getAccountInfoSync() : {};
const envVersion = accountInfo.miniProgram ? accountInfo.miniProgram.envVersion : 'develop';
BASE_URL = envVersion === 'trial' || envVersion === 'release' ? PROD_BASE_URL : DEV_BASE_URL;
// #endif

// #ifndef MP-WEIXIN
BASE_URL = process.env.NODE_ENV === 'production' ? PROD_BASE_URL : DEV_BASE_URL;
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
