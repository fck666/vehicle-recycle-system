<template>
  <view class="container">
    <view class="header">
      <text class="title">车辆拆解回收系统</text>
    </view>
    <view class="menu-grid">
      <view class="menu-item" @click="goToPrice">
        <text class="menu-text">今日行情</text>
      </view>
      <view class="menu-item" @click="goToValuation">
        <text class="menu-text">车辆估值</text>
      </view>
      <view class="menu-item" @click="goToLogin">
        <text class="menu-text">登录/绑定</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const goToPrice = () => {
  uni.navigateTo({ url: '/pages/price/price' });
};
const goToValuation = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' });
};
const goToLogin = () => {
  uni.navigateTo({ url: '/pages/login/login' });
};
</script>

<style>
.container { padding: 20px; }
.header { text-align: center; margin-bottom: 30px; margin-top: 20px; }
.title { font-size: 20px; font-weight: bold; color: #333; }
.menu-grid { display: flex; flex-wrap: wrap; justify-content: space-between; }
.menu-item { width: 48%; height: 100px; background-color: #f8f8f8; margin-bottom: 15px; display: flex; align-items: center; justify-content: center; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.menu-text { font-size: 16px; color: #333; font-weight: 500; }
</style>
