<template>
  <view class="container">
    <view class="header">今日材料行情</view>
    
    <view v-if="loading" class="loading">加载中...</view>
    
    <view v-else class="price-list">
      <view v-for="(item, index) in prices" :key="index" class="price-item" @click="goHistory(item)">
        <view class="info">
          <text class="name">{{ getMaterialName(item) }}</text>
          <text class="date">{{ item.effectiveDate || '无日期' }}</text>
        </view>
        <view class="value">
          <text class="price-num">{{ getPrice(item) }}</text>
          <text class="unit">{{ getUnit(item) }}</text>
          <text class="hint">查看历史</text>
        </view>
      </view>
    </view>
    
    <view class="empty" v-if="!loading && prices.length === 0">暂无数据</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../../utils/request';

const prices = ref([]);
const loading = ref(true);
const materialNameMap = {
  steel: '钢材',
  aluminum: '铝材',
  copper: '铜材',
  battery: '电池',
  plastic: '塑料',
  rubber: '橡胶',
  others: '其他材料'
};
const sourceNameMap = ref({});

onMounted(() => {
  fetchPrices();
});

const fetchPrices = () => {
  loading.value = true;
  Promise.all([
    request({ url: '/material-prices' }),
    request({ url: '/material-sources' }).catch(() => [])
  ])
    .then(([priceRes, sourceRes]) => {
      prices.value = priceRes || [];
      sourceNameMap.value = buildSourceNameMap(sourceRes || []);
    })
    .catch(err => {
      console.error(err);
      uni.showToast({ title: '获取价格失败', icon: 'none' });
    })
    .finally(() => {
      loading.value = false;
    });
};

const buildSourceNameMap = (sources) => {
  return (sources || []).reduce((acc, item) => {
    const type = (item?.type || '').toString().toLowerCase();
    if (!type) {
      return acc;
    }
    acc[type] = item?.displayName || '';
    return acc;
  }, {});
};

const getMaterialName = (item) => {
  const type = (item?.type || '').toString().toLowerCase();
  const displayName = sourceNameMap.value[type] || materialNameMap[type] || item?.materialName || '';
  if (displayName && item?.type) {
    return `${displayName} (${item.type})`;
  }
  return item?.type || '未知材料';
};

const getPrice = (item) => {
  if (item?.price !== undefined && item?.price !== null && item?.price !== '') {
    return item.price;
  }
  if (item?.pricePerKg !== undefined && item?.pricePerKg !== null && item?.pricePerKg !== '') {
    return item.pricePerKg;
  }
  return '--';
};

const getUnit = (item) => {
  const unit = item?.unit || 'CNY/KG';
  if (unit.includes('/')) {
    return unit;
  }
  return `元/${unit}`;
};

const goHistory = (item) => {
  const type = item?.type || '';
  if (!type) {
    return;
  }
  const name = encodeURIComponent(getMaterialName(item));
  const unit = encodeURIComponent(getUnit(item));
  uni.navigateTo({
    url: `/pages/price/history?type=${encodeURIComponent(type)}&name=${name}&unit=${unit}`
  });
};
</script>

<style>
.container { background-color: #f5f5f5; min-height: 100vh; padding-bottom: 20px; }
.header { padding: 15px; font-size: 18px; font-weight: bold; background: #fff; border-bottom: 1px solid #eee; }
.price-list { margin-top: 10px; }
.price-item { display: flex; justify-content: space-between; align-items: center; padding: 15px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.info { display: flex; flex-direction: column; }
.name { font-size: 16px; font-weight: 500; color: #333; margin-bottom: 4px; }
.date { font-size: 12px; color: #999; }
.value { text-align: right; }
.price-num { font-size: 18px; color: #e54d42; font-weight: bold; margin-right: 4px; }
.unit { font-size: 12px; color: #666; }
.hint { display: block; margin-top: 4px; font-size: 12px; color: #07c160; }
.loading, .empty { text-align: center; padding: 30px; color: #999; }
</style>
