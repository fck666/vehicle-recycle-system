<template>
  <view class="container">
    <view class="header">
      <text class="title">{{ materialName }}</text>
      <text class="sub">历史价格</text>
    </view>
    <view class="toolbar">
      <picker mode="date" :value="fromDate" @change="onFromDateChange">
        <view class="date-btn">开始：{{ fromDate }}</view>
      </picker>
      <picker mode="date" :value="toDate" @change="onToDateChange">
        <view class="date-btn">结束：{{ toDate }}</view>
      </picker>
      <button class="query-btn" size="mini" type="primary" @click="fetchHistory">查询</button>
    </view>
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="historyList.length === 0" class="empty">暂无历史数据</view>
    <view v-else class="list">
      <view v-for="(item, index) in historyList" :key="index" class="item">
        <view class="left">
          <text class="date">{{ item.effectiveDate || '--' }}</text>
          <text class="source">{{ item.sourceName || '未知来源' }}</text>
        </view>
        <view class="right">
          <text class="price">{{ item.pricePerKg ?? '--' }}</text>
          <text class="unit">{{ unit }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '../../utils/request';

const type = ref('');
const materialName = ref('材料');
const unit = ref('CNY/KG');
const historyList = ref([]);
const loading = ref(false);
function formatDate(date) {
  const y = date.getFullYear();
  const m = `${date.getMonth() + 1}`.padStart(2, '0');
  const d = `${date.getDate()}`.padStart(2, '0');
  return `${y}-${m}-${d}`;
}
const today = new Date();
const toDate = ref(formatDate(today));
const fromDate = ref(formatDate(new Date(today.getTime() - 90 * 24 * 60 * 60 * 1000)));

const onFromDateChange = (e) => {
  fromDate.value = e.detail.value;
};

const onToDateChange = (e) => {
  toDate.value = e.detail.value;
};

const fetchHistory = () => {
  if (!type.value) {
    return;
  }
  loading.value = true;
  request({
    url: `/material-prices/${encodeURIComponent(type.value)}/history?from=${fromDate.value}&to=${toDate.value}`
  })
    .then((res) => {
      historyList.value = res || [];
    })
    .catch((err) => {
      console.error(err);
      uni.showToast({ title: '获取历史失败', icon: 'none' });
    })
    .finally(() => {
      loading.value = false;
    });
};

onLoad((options) => {
  type.value = options?.type || '';
  materialName.value = decodeURIComponent(options?.name || '材料');
  unit.value = decodeURIComponent(options?.unit || 'CNY/KG');
  fetchHistory();
});
</script>

<style>
.container { min-height: 100vh; background: #f5f5f5; }
.header { background: #fff; padding: 16px; border-bottom: 1px solid #eee; }
.title { display: block; font-size: 18px; font-weight: 700; color: #222; }
.sub { display: block; margin-top: 4px; font-size: 13px; color: #888; }
.toolbar { display: flex; align-items: center; gap: 8px; background: #fff; padding: 10px 12px; margin-top: 8px; }
.date-btn { font-size: 12px; color: #333; background: #f6f6f6; padding: 6px 8px; border-radius: 4px; }
.query-btn { margin-left: auto; }
.loading, .empty { text-align: center; padding: 32px 0; color: #999; }
.list { margin-top: 8px; }
.item { background: #fff; display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #f0f0f0; }
.left { display: flex; flex-direction: column; }
.date { font-size: 15px; color: #333; font-weight: 500; }
.source { margin-top: 4px; font-size: 12px; color: #888; }
.right { text-align: right; }
.price { font-size: 20px; color: #e54d42; font-weight: bold; margin-right: 4px; }
.unit { font-size: 12px; color: #666; }
</style>
