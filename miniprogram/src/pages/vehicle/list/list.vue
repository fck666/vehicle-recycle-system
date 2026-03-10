<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-box">
      <input class="search-input" v-model="queryParams.q" placeholder="搜索品牌、型号、产品号..." confirm-type="search" @confirm="handleSearch" />
      <view class="search-btn" @click="handleSearch">搜索</view>
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
import { onReachBottom } from '@dcloudio/uni-app';
import request from '../../../utils/request';

const vehicleList = ref([]);
const loading = ref(false);
const finished = ref(false);
const facets = reactive({ brands: [] });

const queryParams = reactive({
  page: 0,
  size: 10,
  q: '',
  brand: ''
});

onMounted(() => {
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
  loadData(true);
};

const selectBrand = (brand) => {
  queryParams.brand = brand;
  loadData(true);
};

const goToDetail = (id) => {
  uni.navigateTo({ url: '/pages/vehicle/detail/detail?id=' + id });
};
</script>

<style>
.container { background-color: #f5f5f5; min-height: 100vh; }
.search-box { display: flex; align-items: center; padding: 10px 15px; background-color: #fff; position: sticky; top: 0; z-index: 100; }
.search-input { flex: 1; height: 36px; background-color: #f2f2f2; border-radius: 18px; padding: 0 15px; font-size: 14px; }
.search-btn { margin-left: 10px; color: #07c160; font-size: 16px; font-weight: 500; }

.filter-scroll { white-space: nowrap; background-color: #fff; padding: 10px 15px; border-bottom: 1px solid #eee; }
.filter-item { display: inline-block; padding: 5px 15px; background-color: #f2f2f2; border-radius: 15px; margin-right: 10px; font-size: 13px; color: #666; }
.filter-item.active { background-color: #e8f9f0; color: #07c160; font-weight: 500; }

.vehicle-list { padding: 15px; }
.vehicle-item { background-color: #fff; border-radius: 8px; padding: 15px; margin-bottom: 15px; }
.item-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid #f9f9f9; }
.brand-model { font-size: 16px; font-weight: bold; color: #333; }
.batch-tag { font-size: 12px; color: #999; background-color: #f2f2f2; padding: 2px 6px; border-radius: 4px; }
.info-row { display: flex; margin-bottom: 5px; font-size: 14px; }
.label { color: #999; width: 80px; }
.value { color: #333; flex: 1; }

.loading-more, .no-more, .empty { text-align: center; padding: 15px; color: #999; font-size: 14px; }
</style>
