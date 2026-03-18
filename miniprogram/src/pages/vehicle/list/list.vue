<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-box">
      <input class="search-input" v-model="queryParams.q" placeholder="搜索品牌、型号、产品号..." confirm-type="search" @confirm="handleSearch" />
      <view class="search-btn" @click="handleSearch">搜索</view>
    </view>
    <view class="history-box" v-if="searchHistory.length">
      <view class="history-header">
        <text class="history-title">搜索历史</text>
        <text class="history-clear" @click="clearSearchHistory">清空</text>
      </view>
      <view class="history-list">
        <view class="history-item" v-for="item in searchHistory" :key="item" @click="applySearchHistory(item)">
          {{ item }}
        </view>
      </view>
    </view>

    <!-- 筛选标签 -->
    <scroll-view scroll-x class="filter-scroll" v-if="facets.brands.length > 0">
      <view class="filter-item" :class="{ active: !queryParams.brand }" @click="selectBrand('')">全部品牌</view>
      <view class="filter-item" :class="{ active: queryParams.brand === brand }" v-for="brand in facets.brands" :key="brand" @click="selectBrand(brand)">
        {{ brand }}
      </view>
    </scroll-view>

    <!-- 列表内容 -->
    <view class="vehicle-list">
      <view class="vehicle-item" v-for="item in vehicleList" :key="item.id" @click="goToDetail(item.id)">
        <view class="item-header">
          <text class="brand-model">{{ item.brand }} {{ item.model }}</text>
          <text class="batch-tag">第{{ item.batchNo }}批</text>
        </view>
        <view class="item-body">
          <view class="info-row">
            <text class="label">产品型号：</text>
            <text class="value">{{ item.productModel }}</text>
          </view>
          <view class="info-row">
            <text class="label">车辆类型：</text>
            <text class="value">{{ item.vehicleType }}</text>
          </view>
          <view class="info-row">
            <text class="label">整备质量：</text>
            <text class="value">{{ item.curbWeight }} kg</text>
          </view>
        </view>
      </view>
      
      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">加载中...</view>
      <view class="no-more" v-if="!loading && finished">没有更多了</view>
      <view class="empty" v-if="!loading && vehicleList.length === 0">暂无数据</view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { onReachBottom, onLoad } from '@dcloudio/uni-app';
import request from '../../../utils/request';

const vehicleList = ref([]);
const loading = ref(false);
const finished = ref(false);
const facets = reactive({ brands: [] });
const searchHistory = ref([]);
const searchHistoryLimit = 10;
const mode = ref('');

onLoad((options) => {
  if (options.mode) {
    mode.value = options.mode;
  }
});

const queryParams = reactive({
  page: 0,
  size: 10,
  q: '',
  brand: ''
});

const getSearchHistoryKey = () => {
  const userId = uni.getStorageSync('userId') || 'anonymous';
  const prefix = mode.value ? `mp_vehicle_search_${mode.value}_history_` : 'mp_vehicle_search_history_';
  return `${prefix}${userId}`;
};

const loadSearchHistory = () => {
  const key = getSearchHistoryKey();
  const list = uni.getStorageSync(key);
  if (!Array.isArray(list)) {
    searchHistory.value = [];
    return;
  }
  searchHistory.value = list
    .filter(v => typeof v === 'string')
    .map(v => v.trim())
    .filter(Boolean)
    .slice(0, searchHistoryLimit);
};

const saveSearchHistory = () => {
  uni.setStorageSync(getSearchHistoryKey(), searchHistory.value);
};

const recordSearchHistory = () => {
  const keyword = (queryParams.q || '').trim();
  if (!keyword) return;
  searchHistory.value = [keyword, ...searchHistory.value.filter(item => item !== keyword)].slice(0, searchHistoryLimit);
  saveSearchHistory();
};

const applySearchHistory = (keyword) => {
  queryParams.q = keyword;
  loadData(true);
};

const clearSearchHistory = () => {
  searchHistory.value = [];
  uni.removeStorageSync(getSearchHistoryKey());
};

onMounted(() => {
  loadSearchHistory();
  fetchFacets();
  loadData(true);
});

onReachBottom(() => {
  if (!finished.value && !loading.value) {
    queryParams.page++;
    loadData();
  }
});

const fetchFacets = () => {
  request({ url: '/vehicles/facets' }).then(res => {
    if (res && res.brands) {
      facets.brands = res.brands;
    }
  });
};

const loadData = (reset = false) => {
  if (loading.value) return;
  if (reset) {
    queryParams.page = 0;
    vehicleList.value = [];
    finished.value = false;
  }
  
  loading.value = true;
  
  const params = {
    page: queryParams.page,
    size: queryParams.size,
    q: queryParams.q
  };
  if (queryParams.brand) {
    params.brands = queryParams.brand;
  }
  if (mode.value === 'dismantle_records') {
    params.hasDismantleRecord = true;
  }

  request({
    url: '/vehicles',
    data: params
  }).then(res => {
    const list = res.content || [];
    if (reset) {
      vehicleList.value = list;
    } else {
      vehicleList.value = [...vehicleList.value, ...list];
    }
    
    if (list.length < queryParams.size) {
      finished.value = true;
    }
  }).catch(err => {
    uni.showToast({ title: '加载失败', icon: 'none' });
  }).finally(() => {
    loading.value = false;
  });
};

const handleSearch = () => {
  recordSearchHistory();
  loadData(true);
};

const selectBrand = (brand) => {
  queryParams.brand = brand;
  loadData(true);
};

const goToDetail = (id) => {
  if (mode.value === 'dismantle_records') {
    uni.navigateTo({ url: '/pages/dismantle/records?vehicleId=' + id });
  } else {
    uni.navigateTo({ url: `/pages/vehicle/detail/detail?id=${id}&mode=${mode.value}` });
  }
};
</script>

<style>
.container { background-color: #f5f5f5; min-height: 100vh; }
.search-box { display: flex; align-items: center; padding: 10px 15px; background-color: #fff; position: sticky; top: 0; z-index: 100; }
.search-input { flex: 1; height: 36px; background-color: #f2f2f2; border-radius: 18px; padding: 0 15px; font-size: 14px; }
.search-btn { margin-left: 10px; color: #07c160; font-size: 16px; font-weight: 500; }
.history-box { background-color: #fff; padding: 8px 15px 12px; border-bottom: 1px solid #eee; }
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.history-title { font-size: 13px; color: #999; }
.history-clear { font-size: 13px; color: #07c160; }
.history-list { display: flex; flex-wrap: wrap; gap: 8px; }
.history-item { background-color: #f2f2f2; border-radius: 14px; padding: 4px 12px; font-size: 12px; color: #666; }

.filter-scroll { white-space: nowrap; background-color: #fff; padding: 10px 15px; border-bottom: 1px solid #eee; }
.filter-item { display: inline-block; padding: 5px 15px; background-color: #f2f2f2; border-radius: 15px; margin-right: 10px; font-size: 13px; color: #666; }
.filter-item.active { background-color: #e8f9f0; color: #07c160; font-weight: 500; }

.vehicle-list { padding: 15px; }
.vehicle-item { background-color: #fff; border-radius: 8px; padding: 15px; margin-bottom: 15px; }
.item-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid #f9f9f9; column-gap: 10px; }
.brand-model { flex: 1; min-width: 0; font-size: 16px; font-weight: bold; color: #333; line-height: 1.35; word-break: break-word; }
.batch-tag { flex-shrink: 0; min-width: 58px; text-align: center; white-space: nowrap; font-size: 12px; color: #999; background-color: #f2f2f2; padding: 2px 6px; border-radius: 4px; }
.info-row { display: flex; margin-bottom: 5px; font-size: 14px; }
.label { color: #999; width: 80px; }
.value { color: #333; flex: 1; }

.loading-more, .no-more, .empty { text-align: center; padding: 15px; color: #999; font-size: 14px; }
</style>
