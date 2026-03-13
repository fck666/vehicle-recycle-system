<template>
  <view class="container">
    <!-- 头部信息 -->
    <view class="header">
      <view class="brand-row">
        <text class="brand">{{ vehicle.brand }}</text>
        <text class="model">{{ vehicle.model }}</text>
      </view>
      <view class="batch-info">
        <text class="tag">第{{ vehicle.batchNo }}批</text>
        <text class="tag primary">{{ vehicle.vehicleType }}</text>
      </view>
    </view>

    <view class="card image-card" v-if="vehicleImages.length > 0">
      <swiper class="image-swiper" indicator-dots circular autoplay :interval="4000" :duration="500">
        <swiper-item v-for="(item, index) in vehicleImages" :key="item.id || index">
          <image class="vehicle-image" :src="item.imageUrl" mode="aspectFill" @click="previewImage(index)" />
        </swiper-item>
      </swiper>
    </view>

    <!-- 基本参数 -->
    <view class="card">
      <view class="card-title">基本参数</view>
      <view class="info-grid">
        <view class="info-item">
          <text class="label">产品型号</text>
          <text class="value">{{ vehicle.productModel || '-' }}</text>
        </view>
        <view class="info-item">
          <text class="label">整备质量</text>
          <text class="value">{{ vehicle.curbWeight ? vehicle.curbWeight + ' kg' : '-' }}</text>
        </view>
        <view class="info-item">
          <text class="label">燃料类型</text>
          <text class="value">{{ vehicle.fuelType || '-' }}</text>
        </view>
        <view class="info-item">
          <text class="label">电池容量</text>
          <text class="value">{{ vehicle.batteryKwh ? vehicle.batteryKwh + ' kWh' : '-' }}</text>
        </view>
        <view class="info-item full-width">
          <text class="label">生产厂家</text>
          <text class="value">{{ vehicle.manufacturerName || '-' }}</text>
        </view>
      </view>
    </view>

    <!-- 估值结果 (仅登录可见) -->
    <view class="card" v-if="isLoggedIn">
      <view class="card-title">
        <text>参考估值</text>
        <view class="refresh-btn" @click="calculateValuation">重新计算</view>
      </view>
      
      <view class="valuation-result" v-if="valuation">
        <view class="total-price">
          <text class="currency">¥</text>
          <text class="amount">{{ valuation.totalValue }}</text>
        </view>
        <view class="breakdown">
          <view class="breakdown-item">
            <text class="bd-label">废钢</text>
            <text class="bd-value">¥{{ valuation.steelValue }}</text>
          </view>
          <view class="breakdown-item">
            <text class="bd-label">废铝</text>
            <text class="bd-value">¥{{ valuation.aluminumValue }}</text>
          </view>
          <view class="breakdown-item">
            <text class="bd-label">废铜</text>
            <text class="bd-value">¥{{ valuation.copperValue }}</text>
          </view>
          <view class="breakdown-item">
            <text class="bd-label">电池</text>
            <text class="bd-value">¥{{ valuation.batteryValue }}</text>
          </view>
        </view>
      </view>
      <view class="empty-valuation" v-else>
        <text>暂无估值数据，请点击计算</text>
      </view>
    </view>
    
    <view class="login-tip" v-else @click="goToLogin">
      登录后查看估值信息 >
    </view>

    <!-- 底部按钮 -->
    <view class="footer-bar" v-if="isStaff">
      <button class="action-btn" type="primary" @click="goToDismantle">录入拆解数据</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request, { API_BASE_URL } from '../../../utils/request';

const vehicleId = ref(null);
const vehicle = ref({});
const valuation = ref(null);
const isLoggedIn = ref(false);
const isStaff = ref(false);
const valuationLoading = ref(false);
const vehicleImages = computed(() => {
  const list = Array.isArray(vehicle.value?.images) ? vehicle.value.images : [];
  return list
    .filter(item => item?.imageUrl)
    .map(item => ({
      ...item,
      imageUrl: `${API_BASE_URL}/vehicle-images/proxy?source=${encodeURIComponent(item.imageUrl)}`
    }))
    .sort((a, b) => (a?.sortOrder || 0) - (b?.sortOrder || 0));
});

onLoad((options) => {
  vehicleId.value = options.id;
  checkLogin();
  loadDetail();
});

const checkLogin = () => {
  const token = uni.getStorageSync('token');
  isLoggedIn.value = !!token;
  if (token) {
    const roles = uni.getStorageSync('roles') || [];
    isStaff.value = roles.includes('ADMIN') || roles.includes('OPERATOR');
  }
};

const loadDetail = () => {
  request({ url: '/vehicles/' + vehicleId.value })
    .then(res => {
      vehicle.value = res;
      if (isLoggedIn.value) {
        calculateValuation();
      }
    })
    .catch(() => {
      uni.showToast({ title: '车型详情加载失败', icon: 'none' });
    });
};

const calculateValuation = () => {
  if (valuationLoading.value) {
    return;
  }
  valuationLoading.value = true;
  uni.showLoading({ title: '计算中...' });
  request({
    url: '/valuation/' + vehicleId.value,
    method: 'POST'
  }).then(res => {
    valuation.value = res;
  }).catch(() => {
    valuation.value = null;
    uni.showToast({ title: '该车型暂无可用估值', icon: 'none' });
  }).finally(() => {
    valuationLoading.value = false;
    uni.hideLoading();
  });
};

const goToLogin = () => {
  uni.navigateTo({ url: '/pages/login/login' });
};

const goToDismantle = () => {
  uni.navigateTo({ url: '/pages/dismantle/entry?vehicleId=' + vehicleId.value });
};

const previewImage = (index) => {
  const urls = vehicleImages.value.map(item => item.imageUrl);
  if (urls.length === 0) {
    return;
  }
  uni.previewImage({
    urls,
    current: urls[index] || urls[0]
  });
};
</script>

<style>
.container { background-color: #f5f5f5; min-height: 100vh; padding-bottom: 80px; }
.header { background-color: #fff; padding: 20px; margin-bottom: 10px; }
.brand-row { margin-bottom: 10px; }
.brand { font-size: 20px; font-weight: bold; margin-right: 10px; color: #333; }
.model { font-size: 18px; color: #666; }
.batch-info { display: flex; gap: 10px; }
.tag { font-size: 12px; background-color: #f2f2f2; padding: 2px 8px; border-radius: 4px; color: #666; }
.tag.primary { background-color: #e8f9f0; color: #07c160; }

.card { background-color: #fff; margin: 10px; border-radius: 8px; padding: 15px; }
.image-card { padding: 0; overflow: hidden; }
.image-swiper { width: 100%; height: 220px; }
.vehicle-image { width: 100%; height: 100%; }
.card-title { font-size: 16px; font-weight: bold; border-left: 4px solid #07c160; padding-left: 10px; margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center; }
.refresh-btn { font-size: 12px; color: #07c160; font-weight: normal; border: 1px solid #07c160; padding: 2px 8px; border-radius: 12px; }

.info-grid { display: flex; flex-wrap: wrap; }
.info-item { width: 50%; margin-bottom: 10px; display: flex; flex-direction: column; }
.full-width { width: 100%; }
.info-item .label { font-size: 12px; color: #999; margin-bottom: 4px; }
.info-item .value { font-size: 14px; color: #333; }

.valuation-result { text-align: center; padding: 10px 0; }
.total-price { margin-bottom: 20px; color: #fa5151; }
.currency { font-size: 20px; font-weight: bold; }
.amount { font-size: 32px; font-weight: bold; }
.breakdown { display: flex; justify-content: space-around; background-color: #f9f9f9; padding: 15px; border-radius: 8px; }
.breakdown-item { display: flex; flex-direction: column; align-items: center; }
.bd-label { font-size: 12px; color: #999; margin-bottom: 5px; }
.bd-value { font-size: 14px; font-weight: bold; color: #333; }

.login-tip { text-align: center; padding: 20px; color: #07c160; font-size: 14px; }

.footer-bar { position: fixed; bottom: 0; left: 0; right: 0; background-color: #fff; padding: 10px 20px; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); }
.action-btn { border-radius: 22px; }
</style>
