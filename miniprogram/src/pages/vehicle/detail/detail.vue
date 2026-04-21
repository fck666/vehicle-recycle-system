<template>
  <view class="container">
    <!-- 头部信息 -->
    <view class="header">
      <view class="brand-row">
        <text class="brand">{{ vehicle.brand }}</text>
        <text class="model">{{ vehicle.model }}</text>
      </view>
      <view class="batch-info">
        <text class="tag">ID {{ vehicle.id || '-' }}</text>
        <text class="tag">第{{ vehicle.batchNo }}批</text>
        <text class="tag">{{ sourceTypeText }}</text>
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
          <text class="label">总质量</text>
          <text class="value">{{ vehicle.grossWeight ? vehicle.grossWeight + ' kg' : '-' }}</text>
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
        <view class="info-item">
          <text class="label">发布日期</text>
          <text class="value">{{ vehicle.releaseDate || '-' }}</text>
        </view>
        <view class="info-item">
          <text class="label">年份</text>
          <text class="value">{{ vehicle.modelYear || '-' }}</text>
        </view>
        <view class="info-item">
          <text class="label">产品ID</text>
          <text class="value">{{ vehicle.productId || '-' }}</text>
        </view>
        <view class="info-item">
          <text class="label">产品号</text>
          <text class="value">{{ vehicle.productNo || '-' }}</text>
        </view>
      </view>
    </view>

    <view class="card" v-if="vehicleDocuments.length > 0">
      <view class="card-title">文档信息</view>
      <view class="doc-list">
        <view class="doc-item" v-for="(doc, index) in vehicleDocuments" :key="doc.id || index">
          <text class="doc-name">{{ doc.docName || '未命名文档' }}</text>
          <text class="doc-meta">类型：{{ doc.docType || '-' }}</text>
          <text class="doc-meta">抓取时间：{{ doc.fetchedAt || '-' }}</text>
          <text class="doc-meta">来源：{{ doc.sourceUrl || '-' }}</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">
        <text>同车系参考</text>
        <text class="series-summary" v-if="sameSeries">高置信 {{ sameSeries.highConfidenceCount }} / 中置信 {{ sameSeries.mediumConfidenceCount }}</text>
      </view>
      <view class="series-loading" v-if="sameSeriesLoading">加载中...</view>
      <view class="series-empty" v-else-if="!sameSeries || !sameSeries.candidates || sameSeries.candidates.length === 0">暂无同车系候选</view>
      <view class="series-list" v-else>
        <view class="series-item" v-for="item in sameSeries.candidates" :key="item.vehicleId" @click="goToDetail(item.vehicleId)">
          <view class="series-main">
            <text class="series-title">#{{ item.vehicleId }} {{ item.brand }} {{ item.model }}</text>
            <view class="series-tags">
              <text class="series-tag" :class="item.confidenceLevel === 'HIGH' ? 'high' : 'medium'">{{ item.confidenceLevel === 'HIGH' ? '高置信' : '中置信' }}</text>
              <text class="series-score">得分 {{ item.score }}</text>
              <text class="series-score">{{ item.modelYear }}款</text>
              <text class="series-score" style="color:#07c160;">查看详情 ›</text>
            </view>
          </view>
          <text class="series-reason">{{ item.matchReasons && item.matchReasons.length ? item.matchReasons.join('、') : '暂无匹配依据' }}</text>
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
        <view style="font-size: 12px; color: #999; text-align: center; margin-bottom: 15px;">综合估值结果</view>

        <view class="dimension-box">
          <view class="dim-header">
            <text class="dim-title" style="color: #07c160;">● 精准匹配估值</text>
          </view>
          <view v-if="valuation.exactMatch && valuation.exactMatch.recordCount > 0" class="dim-content">
            <view class="dim-row"><text class="dim-label">平均价:</text><text class="dim-val strong">¥{{ valuation.exactMatch.avgValue }}</text></view>
            <view class="dim-row"><text class="dim-label">价格区间:</text><text class="dim-val">¥{{ valuation.exactMatch.minValue }} ~ ¥{{ valuation.exactMatch.maxValue }}</text></view>
            <view class="dim-tip">基于 {{ valuation.exactMatch.recordCount }} 条同产品号/车型的拆解记录计算。</view>
          </view>
          <view v-else class="dim-empty">暂无完全匹配的拆解记录</view>
        </view>

        <view class="dimension-box">
          <view class="dim-header">
            <text class="dim-title" style="color: #1989fa;">● 同车系参考估值 (高置信)</text>
          </view>
          <view v-if="valuation.seriesHighMatch && valuation.seriesHighMatch.recordCount > 0" class="dim-content">
            <view class="dim-row"><text class="dim-label">平均价:</text><text class="dim-val strong">¥{{ valuation.seriesHighMatch.avgValue }}</text></view>
            <view class="dim-row"><text class="dim-label">价格区间:</text><text class="dim-val">¥{{ valuation.seriesHighMatch.minValue }} ~ ¥{{ valuation.seriesHighMatch.maxValue }}</text></view>
            <view class="dim-tip">基于 {{ valuation.seriesHighMatch.recordCount }} 条高置信度的同车系拆解记录计算。</view>
          </view>
          <view v-else class="dim-empty">暂无同车系的拆解记录 (高置信)</view>
        </view>

        <view class="dimension-box">
          <view class="dim-header">
            <text class="dim-title" style="color: #909399;">● 同车系参考估值 (中置信)</text>
          </view>
          <view v-if="valuation.seriesMediumMatch && valuation.seriesMediumMatch.recordCount > 0" class="dim-content">
            <view class="dim-row"><text class="dim-label">平均价:</text><text class="dim-val strong">¥{{ valuation.seriesMediumMatch.avgValue }}</text></view>
            <view class="dim-row"><text class="dim-label">价格区间:</text><text class="dim-val">¥{{ valuation.seriesMediumMatch.minValue }} ~ ¥{{ valuation.seriesMediumMatch.maxValue }}</text></view>
            <view class="dim-tip">基于 {{ valuation.seriesMediumMatch.recordCount }} 条中等置信度的同车系拆解记录计算。</view>
          </view>
          <view v-else class="dim-empty">暂无同车系的拆解记录 (中置信)</view>
        </view>

        <view class="valuation-notice">
          注：以上估值已按照今日最新大盘行情重新核算拆解记录中的各项材料价值。拆解明细中标注了“个体差异/溢价”的部件未计入统计。
        </view>
      </view>
      <view class="empty-valuation" v-else>
        <text>暂无估值数据，请点击计算</text>
      </view>
    </view>
    
    <view class="login-tip" v-else @click="goToLogin">
      登录后查看估值信息 ›
    </view>

    <!-- 底部按钮 -->
    <view class="footer-bar" v-if="isStaff && mode === 'dismantle'">
      <button class="action-btn" type="primary" @click="goToDismantle">录入拆解数据</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request, { API_BASE_URL } from '../../../utils/request';

const vehicleId = ref(null);
const mode = ref('');
const vehicle = ref({});
const valuation = ref(null);
const sameSeries = ref(null);
const sameSeriesLoading = ref(false);
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
const vehicleDocuments = computed(() => {
  const list = Array.isArray(vehicle.value?.documents) ? vehicle.value.documents : [];
  return list.filter(item => item?.docUrl);
});
const sourceTypeText = computed(() => {
  const map = {
    CRAWLED: '系统采集',
    MANUAL: '手动录入',
    EDITED: '采集后编辑'
  };
  return map[vehicle.value?.sourceType] || '未知来源';
});

onLoad((options) => {
  vehicleId.value = options.id;
  if (options.mode) {
    mode.value = options.mode;
  }
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
  request({
    url: '/vehicles/' + vehicleId.value,
    data: {
      signMedia: false
    }
  })
    .then(res => {
      vehicle.value = res;
      loadSameSeries();
      if (isLoggedIn.value) {
        calculateValuation();
      }
    })
    .catch(() => {
      uni.showToast({ title: '车型详情加载失败', icon: 'none' });
    });
};

const loadSameSeries = () => {
  sameSeriesLoading.value = true;
  request({
    url: '/vehicles/' + vehicleId.value + '/same-series',
    data: {
      yearWindow: 4,
      limit: 10
    }
  }).then(res => {
    sameSeries.value = res;
  }).catch(() => {
    sameSeries.value = null;
  }).finally(() => {
    sameSeriesLoading.value = false;
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

const goToDetail = (id) => {
  uni.navigateTo({ url: `/pages/vehicle/detail/detail?id=${id}&mode=${mode.value || ''}` });
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
.doc-list { display: flex; flex-direction: column; gap: 12px; }
.doc-item { background-color: #f9f9f9; border-radius: 8px; padding: 10px 12px; display: flex; flex-direction: column; gap: 4px; }
.doc-name { font-size: 14px; color: #333; font-weight: 600; }
.doc-meta { font-size: 12px; color: #666; }
.series-summary { font-size: 12px; color: #999; font-weight: normal; }
.series-loading, .series-empty { color: #999; font-size: 13px; }
.series-list { display: flex; flex-direction: column; gap: 10px; }
.series-item { background-color: #f9f9f9; border-radius: 8px; padding: 10px 12px; display: flex; flex-direction: column; gap: 6px; }
.series-main { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.series-title { font-size: 14px; font-weight: 600; color: #333; flex: 1; }
.series-tags { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.series-tag { font-size: 11px; border-radius: 10px; padding: 2px 8px; color: #fff; }
.series-tag.high { background-color: #07c160; }
.series-tag.medium { background-color: #ff9900; }
.series-score { font-size: 12px; color: #666; }
.series-reason { font-size: 12px; color: #666; }

.valuation-result { background-color: #fdfaf5; padding: 20px; border-radius: 8px; border: 1px solid #fbeedb; }
.total-price { text-align: center; color: #ff9800; margin-bottom: 5px; }
.currency { font-size: 20px; margin-right: 5px; font-weight: bold; }
.amount { font-size: 36px; font-weight: bold; }

.dimension-box { background: #fff; border: 1px solid #eee; border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.dim-header { margin-bottom: 8px; }
.dim-title { font-size: 14px; font-weight: bold; }
.dim-content { font-size: 13px; color: #333; }
.dim-row { display: flex; margin-bottom: 4px; }
.dim-label { width: 70px; color: #666; }
.dim-val { flex: 1; }
.dim-val.strong { font-size: 15px; font-weight: bold; color: #333; }
.dim-tip { font-size: 11px; color: #999; margin-top: 6px; border-top: 1px dashed #eee; padding-top: 6px; }
.dim-empty { font-size: 12px; color: #999; text-align: center; padding: 10px 0; }

.valuation-notice { font-size: 11px; color: #ff9800; background: #fff3e0; padding: 8px; border-radius: 4px; line-height: 1.4; margin-top: 10px; }
.empty-valuation { text-align: center; color: #999; padding: 30px 0; font-size: 14px; }

.login-tip { text-align: center; padding: 20px; color: #07c160; font-size: 14px; }

.footer-bar { position: fixed; bottom: 0; left: 0; right: 0; background-color: #fff; padding: 10px 20px; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); }
.action-btn { border-radius: 22px; }
</style>
