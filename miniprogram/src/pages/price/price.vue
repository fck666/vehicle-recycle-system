<template>
  <view class="container">
    <view class="header">今日材料行情</view>
    
    <view v-if="loading" class="loading">加载中...</view>
    
    <view v-else class="price-list">
      <view v-for="(item, index) in prices" :key="index" class="price-item">
        <view class="info">
          <text class="name">{{ item.materialName || '未知材料' }}</text>
          <text class="date">{{ item.effectiveDate || '无日期' }}</text>
        </view>
        <view class="value">
          <text class="price-num">{{ item.price }}</text>
          <text class="unit">元/{{ item.unit || 'kg' }}</text>
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

onMounted(() => {
  fetchPrices();
});

const fetchPrices = () => {
  loading.value = true;
  request({ url: '/material-prices' })
    .then(res => {
      // 后端返回的应该是一个数组
      prices.value = res || [];
    })
    .catch(err => {
      console.error(err);
      uni.showToast({ title: '获取价格失败', icon: 'none' });
    })
    .finally(() => {
      loading.value = false;
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
.loading, .empty { text-align: center; padding: 30px; color: #999; }
</style>
