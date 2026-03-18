const DEV_BASE_URL = 'http://localhost:8090/api';
const PROD_BASE_URL = 'https://api.xhyscrapcar.com/api';

// 获取当前环境信息
const accountInfo = uni.getAccountInfoSync();
// envVersion: 'develop' (开发版), 'trial' (体验版), 'release' (正式版)
const envVersion = accountInfo.miniProgram ? accountInfo.miniProgram.envVersion : 'develop';

let BASE_URL;

// 1. 如果是 HBuilderX 运行到浏览器/模拟器，通常 process.env.NODE_ENV 会生效
if (process.env.NODE_ENV === 'development') {
  BASE_URL = DEV_BASE_URL;
} else {
  // 2. 如果是真机/上传版本，根据微信环境判断
  if (envVersion === 'develop') {
    // 真机开发版（开启调试时），也可以尝试连本地（需要手机和电脑在同一局域网并把localhost换成IP），
    // 但为了方便，真机开发版通常建议也连生产库，或者手动改为本地IP。
    // 这里暂时保持连 DEV，但请注意手机无法访问 localhost。
    // 如果你在手机预览时遇到连接错误，请手动将 DEV_BASE_URL 改为电脑局域网IP。
    BASE_URL = DEV_BASE_URL;
  } else {
    // 体验版(trial) 和 正式版(release) 强制使用生产环境
    BASE_URL = PROD_BASE_URL;
  }
}

// 强制修正：如果是在 HBuilderX 中点“运行”但选了“生产环境”，或者打包时
// 为了避免歧义，我们打印一下当前生效的 URL
console.log('Current Env:', envVersion, 'Node Env:', process.env.NODE_ENV, 'API URL:', BASE_URL);

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
