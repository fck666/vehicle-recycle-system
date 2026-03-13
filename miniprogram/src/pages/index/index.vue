<template>
  <view class="container">
    <view class="banner">
      <view class="banner-content">
        <text class="banner-title">车辆拆解回收系统</text>
        <text class="banner-desc">实时掌握行情，精准评估价值</text>
      </view>
    </view>

    <view class="section-title">核心业务</view>
    <view class="menu-grid">
      <view class="menu-item" @click="goToPrice">
        <view class="icon-box price-icon">
          <image class="icon-img" src="/static/icons/market.svg" mode="aspectFit"></image>
        </view>
        <text class="menu-text">今日行情</text>
      </view>
      <view class="menu-item" @click="goToValuation">
        <view class="icon-box valuation-icon">
          <image class="icon-img" src="/static/icons/valuation.svg" mode="aspectFit"></image>
        </view>
        <text class="menu-text">车辆估值</text>
      </view>
    </view>

    <view class="section-title" v-if="isStaff">管理后台</view>
    <view class="menu-grid" v-if="isStaff">
      <view class="menu-item full-width" @click="goToDismantle">
        <view class="icon-box dismantle-icon">
          <image class="icon-img" src="/static/icons/dismantle.svg" mode="aspectFit"></image>
        </view>
        <text class="menu-text">拆解录入</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const isStaff = ref(false);

onShow(() => {
  const roles = uni.getStorageSync('roles') || [];
  isStaff.value = roles.includes('ADMIN') || roles.includes('OPERATOR');
});

const goToPrice = () => {
  uni.navigateTo({ url: '/pages/price/price' });
};
const goToValuation = () => {
  uni.navigateTo({ url: '/pages/vehicle/list/list' });
};
const goToDismantle = () => {
  uni.navigateTo({ url: '/pages/vehicle/list/list' });
};
</script>

<style>
.container { background-color: #f8f8f8; min-height: 100vh; padding-bottom: 20px; }
.banner { background-color: #07c160; padding: 40px 20px; border-radius: 0 0 20px 20px; }
.banner-title { font-size: 24px; font-weight: bold; color: #fff; display: block; margin-bottom: 10px; }
.banner-desc { font-size: 14px; color: rgba(255, 255, 255, 0.8); }

.section-title { padding: 20px 15px 10px; font-size: 16px; font-weight: bold; color: #333; }
.menu-grid { display: flex; flex-wrap: wrap; padding: 0 10px; }
.menu-item { 
  width: calc(50% - 20px); 
  height: 120px; 
  background-color: #fff; 
  margin: 10px; 
  display: flex; 
  flex-direction: column;
  align-items: center; 
  justify-content: center; 
  border-radius: 12px; 
  box-shadow: 0 4px 12px rgba(0,0,0,0.05); 
}
.full-width { width: calc(100% - 20px); height: 100px; flex-direction: row; }
.icon-box { width: 40px; height: 40px; background-color: #f0f9eb; border-radius: 10px; margin-bottom: 10px; display: flex; align-items: center; justify-content: center; }
.icon-img { width: 22px; height: 22px; }
.full-width .icon-box { margin-bottom: 0; margin-right: 15px; }
.menu-text { font-size: 15px; color: #333; font-weight: 500; }

.price-icon { background-color: #e8f4ff; }
.valuation-icon { background-color: #fdf6ec; }
.dismantle-icon { background-color: #f0f9eb; }
</style>
