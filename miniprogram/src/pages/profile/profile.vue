<template>
  <view class="container">
    <!-- 头部：展示用户信息或登录提示 -->
    <view class="user-section" @click="handleUserClick">
      <view class="avatar-box">
        <image class="avatar" src="/static/logo.png"></image>
      </view>
      <view class="info-box">
        <text class="username">{{ isLoggedIn ? (isStaff ? '员工: ' + username : username) : '点击登录' }}</text>
        <text class="role-tag" v-if="isLoggedIn">{{ isStaff ? '工作人员' : '普通用户' }}</text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="list-section">
      <view class="list-item" v-if="isLoggedIn && !isStaff" @click="goToBind">
        <text class="list-text">绑定员工账号</text>
        <text class="arrow">></text>
      </view>
      <view class="list-item" @click="handleLogout" v-if="isLoggedIn">
        <text class="list-text">退出登录</text>
        <text class="arrow">></text>
      </view>
      <view class="list-item" @click="showAbout">
        <text class="list-text">关于系统</text>
        <text class="arrow">></text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const isLoggedIn = ref(false);
const isStaff = ref(false);
const username = ref('未登录');

onMounted(() => {
  checkLoginStatus();
});

const checkLoginStatus = () => {
  const token = uni.getStorageSync('token');
  if (token) {
    isLoggedIn.value = true;
    const roles = uni.getStorageSync('roles') || [];
    isStaff.value = roles.includes('ADMIN') || roles.includes('OPERATOR');
    // 这里简单起见从缓存拿，实际可以调 /me 接口
    username.value = '已登录用户'; 
  } else {
    isLoggedIn.value = false;
  }
};

const handleUserClick = () => {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/login' });
  }
};

const goToBind = () => {
  uni.navigateTo({ url: '/pages/login/login' }); // 登录页已有绑定功能
};

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        uni.clearStorageSync();
        isLoggedIn.value = false;
        uni.showToast({ title: '已退出' });
      }
    }
  });
};

const showAbout = () => {
  uni.showModal({
    title: '关于',
    content: '车辆拆解回收系统 v1.0.0',
    showCancel: false
  });
};
</script>

<style>
.container { background-color: #f8f8f8; min-height: 100vh; }
.user-section { background-color: #fff; padding: 30px 20px; display: flex; align-items: center; margin-bottom: 10px; }
.avatar-box { width: 60px; height: 60px; border-radius: 30px; overflow: hidden; background-color: #eee; margin-right: 15px; }
.avatar { width: 100%; height: 100%; }
.info-box { display: flex; flex-direction: column; }
.username { font-size: 18px; font-weight: bold; color: #333; margin-bottom: 5px; }
.role-tag { font-size: 12px; color: #07c160; background: rgba(7, 193, 96, 0.1); padding: 2px 8px; border-radius: 10px; width: fit-content; }

.list-section { background-color: #fff; }
.list-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; border-bottom: 1px solid #f0f0f0; }
.list-text { font-size: 16px; color: #333; }
.arrow { color: #ccc; font-size: 16px; }
</style>
