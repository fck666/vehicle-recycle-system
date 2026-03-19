<template>
  <view class="container">
    <view class="login-box">
      <view class="title">欢迎登录</view>
      <button class="login-btn" type="primary" @click="handleLogin">微信一键登录</button>
      <view class="tips" @click="showBindModal = true">已有员工账号？点击绑定</view>
    </view>

    <!-- 绑定弹窗 -->
    <view class="modal" v-if="showBindModal">
      <view class="modal-content">
        <view class="modal-title">绑定员工账号</view>
        <input class="input" v-model="bindForm.username" placeholder="请输入后台账号" />
        <input class="input" v-model="bindForm.password" password placeholder="请输入密码" />
        <view class="modal-btns">
          <button size="mini" @click="showBindModal = false">取消</button>
          <button size="mini" type="primary" @click="handleBind">绑定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue';
import request from '../../utils/request';

const showBindModal = ref(false);
const bindForm = reactive({
  username: '',
  password: ''
});

const handleLogin = () => {
  uni.showLoading({ title: '登录中...' });
  uni.login({
    provider: 'weixin',
    success: (loginRes) => {
      console.log('Login Code:', loginRes.code);
      
      request({
        url: '/auth/wx/login',
        method: 'POST',
        data: { 
            code: loginRes.code,
            clientType: 'MINIAPP'
        }
      }).then(res => {
        console.log('Login success:', res);
        uni.setStorageSync('token', res.token);
        uni.setStorageSync('userId', res.userId);
        uni.setStorageSync('roles', res.roles);
        uni.setStorageSync('username', res.username); // 缓存用户名
        
        uni.showToast({ title: '登录成功' });
        
        setTimeout(() => {
            uni.reLaunch({ url: '/pages/index/index' });
        }, 1500);
      }).catch(err => {
        console.error('Login failed:', err);
        uni.showToast({ title: '登录失败: ' + (err.message || '未知错误'), icon: 'none' });
      }).finally(() => {
          uni.hideLoading();
      });
    },
    fail: (err) => {
        uni.hideLoading();
        console.error('uni.login failed:', err);
        uni.showToast({ title: '微信登录失败', icon: 'none' });
    }
  });
};

const handleBind = () => {
  if (!bindForm.username || !bindForm.password) {
    uni.showToast({ title: '请填写完整', icon: 'none' });
    return;
  }
  
  // 绑定需要先登录获取 token，这里假设用户已经先点过“微信一键登录”或者我们在绑定接口里也传 code
  // 但为了安全，通常是：先微信登录（成为普通用户）-> 再调用绑定接口（提升权限）
  // 如果用户没登录，无法调用绑定接口（因为需要 Authorization header）
  
  const token = uni.getStorageSync('token');
  if (!token) {
      uni.showToast({ title: '请先点击微信一键登录', icon: 'none' });
      return;
  }

  uni.showLoading({ title: '绑定中...' });
  request({
    url: '/auth/wx/bind',
    method: 'POST',
    data: {
      username: bindForm.username,
      password: bindForm.password
    }
  }).then(() => {
    uni.showToast({ title: '绑定成功，请重新登录' });
    showBindModal.value = false;
    // 绑定成功后，token 可能需要刷新（因为角色变了），简单起见，重新登录
    handleLogin();
  }).catch(err => {
    uni.showToast({ title: '绑定失败: ' + (err.message || '账号或密码错误'), icon: 'none' });
  }).finally(() => {
    uni.hideLoading();
  });
};
</script>

<style>
.container { padding: 40px 30px; display: flex; flex-direction: column; align-items: center; }
.login-box { width: 100%; margin-top: 50px; }
.title { font-size: 24px; font-weight: bold; text-align: center; margin-bottom: 40px; }
.login-btn { width: 100%; height: 48px; line-height: 48px; border-radius: 24px; font-size: 16px; margin-bottom: 20px; }
.tips { text-align: center; margin-top: 20px; color: #576b95; font-size: 14px; text-decoration: underline; }

.modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-content { background: #fff; width: 80%; padding: 20px; border-radius: 10px; }
.modal-title { text-align: center; font-size: 18px; margin-bottom: 20px; font-weight: bold; }
.input { border: 1px solid #ddd; height: 40px; border-radius: 4px; padding: 0 10px; margin-bottom: 15px; }
.modal-btns { display: flex; justify-content: space-around; margin-top: 20px; }
</style>
