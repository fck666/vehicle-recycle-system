<template>
  <view class="container">
    <!-- 头部：展示用户信息或登录提示 -->
    <view class="user-section" @click="handleUserClick">
      <view class="avatar-box">
        <image class="avatar" src="/static/logo.png"></image>
      </view>
      <view class="info-box">
        <view class="username-row">
          <text class="username">{{ isLoggedIn ? (isStaff ? '员工: ' + username : username) : '点击登录' }}</text>
          <text class="edit-icon" v-if="isLoggedIn" @click.stop="showEditModal = true">✎</text>
        </view>
        <text class="role-tag" v-if="isLoggedIn">{{ isStaff ? '工作人员' : '普通用户' }}</text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="list-section">
      <view class="list-item" v-if="isLoggedIn && !isStaff" @click="goToBind">
        <text class="list-text">绑定员工账号</text>
        <text class="arrow">›</text>
      </view>
      <view class="list-item" @click="handleLogout" v-if="isLoggedIn">
        <text class="list-text">退出登录</text>
        <text class="arrow">›</text>
      </view>
      <view class="list-item" @click="showAbout">
        <text class="list-text">关于系统</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 修改用户名弹窗 -->
    <view class="modal" v-if="showEditModal">
      <view class="modal-content">
        <view class="modal-title">修改用户名</view>
        <input class="input" v-model="newUsername" placeholder="请输入新用户名" maxlength="20" />
        <view class="modal-btns">
          <button size="mini" @click="showEditModal = false">取消</button>
          <button size="mini" type="primary" @click="handleUpdateUsername">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../../utils/request';

const isLoggedIn = ref(false);
const isStaff = ref(false);
const username = ref('未登录');

const showEditModal = ref(false);
const newUsername = ref('');

onMounted(() => {
  checkLoginStatus();
});

const checkLoginStatus = () => {
  const token = uni.getStorageSync('token');
  if (token) {
    isLoggedIn.value = true;
    const roles = uni.getStorageSync('roles') || [];
    isStaff.value = roles.includes('ADMIN') || roles.includes('OPERATOR');
    // 从缓存获取真实用户名
    const cachedUsername = uni.getStorageSync('username');
    username.value = cachedUsername ? cachedUsername : '微信用户'; 
    newUsername.value = username.value;
  } else {
    isLoggedIn.value = false;
  }
};

const handleUserClick = () => {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/login' });
  }
};

const handleUpdateUsername = () => {
  if (!newUsername.value || !newUsername.value.trim()) {
    uni.showToast({ title: '用户名不能为空', icon: 'none' });
    return;
  }
  if (newUsername.value === username.value) {
    showEditModal.value = false;
    return;
  }

  uni.showLoading({ title: '保存中...' });
  request({
    url: '/auth/me/username',
    method: 'PUT',
    data: { username: newUsername.value.trim() }
  }).then(() => {
    uni.showToast({ title: '修改成功' });
    username.value = newUsername.value.trim();
    uni.setStorageSync('username', username.value);
    showEditModal.value = false;
  }).catch(err => {
    uni.showToast({ title: '修改失败，可能用户名已存在', icon: 'none' });
  }).finally(() => {
    uni.hideLoading();
  });
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
.username-row { display: flex; align-items: center; margin-bottom: 5px; }
.username { font-size: 18px; font-weight: bold; color: #333; }
.edit-icon { margin-left: 8px; font-size: 16px; color: #999; padding: 2px 5px; }
.role-tag { font-size: 12px; color: #07c160; background: rgba(7, 193, 96, 0.1); padding: 2px 8px; border-radius: 10px; width: fit-content; }

.list-section { background-color: #fff; }
.list-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; border-bottom: 1px solid #f0f0f0; }
.list-text { font-size: 16px; color: #333; }
.arrow { color: #ccc; font-size: 16px; }

/* 弹窗样式 */
.modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-content { background: #fff; width: 80%; padding: 20px; border-radius: 10px; }
.modal-title { text-align: center; font-size: 18px; margin-bottom: 20px; font-weight: bold; }
.input { border: 1px solid #ddd; height: 40px; border-radius: 4px; padding: 0 10px; margin-bottom: 15px; }
.modal-btns { display: flex; justify-content: space-around; margin-top: 20px; }
</style>
