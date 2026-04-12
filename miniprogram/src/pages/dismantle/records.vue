<template>
  <view class="container">
    <view class="header">
      <view class="title">拆解记录</view>
      <view class="subtitle" v-if="vehicle">{{ vehicle.brand }} {{ vehicle.model }}</view>
      <view class="subtitle" v-if="vehicle" style="margin-top: 4px; opacity: 0.7; font-size: 12px;">
        <text style="margin-right: 12px;">产品ID: {{ vehicle.productId || '-' }}</text>
        <text>产品号: {{ vehicle.productNo || '-' }}</text>
      </view>
    </view>

    <view class="loading-state" v-if="loading">
      <text>加载中...</text>
    </view>
    <view class="content" v-else>
      <!-- 当前车型拆解记录 -->
      <view class="section">
        <view class="section-title">当前车型拆解记录</view>
        <view v-if="!records || records.length === 0" class="empty-state">暂无拆解记录</view>
        <view v-else class="record-list">
          <view class="record-card" v-for="item in records" :key="item.id">
            <view class="record-header">
              <text class="record-time">{{ item.createdAt }}</text>
              <text class="record-operator">操作员: {{ item.operatorName || '-' }}</text>
            </view>
            <view class="record-body">
              <view class="material-item" v-for="col in dynamicColumns" :key="col.prop">
                <text class="m-label">{{ col.label }}</text>
                <text class="m-value">{{ item[col.prop] || 0 }} kg</text>
              </view>
              <view class="material-item">
                <text class="m-label">其他</text>
                <text class="m-value">{{ item.otherWeight || 0 }} kg</text>
              </view>
            </view>
            <view class="record-remark" v-if="getFixedPriceText(item)">
              <text class="r-label">固定总价: </text>
              <text class="r-text">{{ getFixedPriceText(item) }}</text>
            </view>
            <view class="part-details" v-if="getPartDetails(item).length > 0">
              <view class="part-section-title">高价值部件明细</view>
              <view class="part-grid">
                <view class="part-row" v-for="(part, pIdx) in getPartDetails(item)" :key="pIdx">
                  <text class="p-name">{{ part.partName }}</text>
                  <text v-if="part.isPremium" class="premium-tag">个体差异</text>
                  <text class="p-count">x{{ part.count }}</text>
                  <text :class="['p-price', part.isPremium ? 'premium-price' : '']" v-if="part.totalPrice > 0">¥{{ part.totalPrice }}</text>
                  <text class="p-remark" v-if="part.remark">({{ part.remark }})</text>
                </view>
              </view>
              <view class="premium-note" v-if="getPartDetails(item).some(p => p.isPremium)">注：标注“个体差异”的部件受实车状况或二手件渠道影响，不计入标准保底回收价参考。</view>
            </view>
            <view class="record-remark" v-if="item.remark">
              <text class="r-label">备注: </text>
              <text class="r-text">{{ item.remark }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 高置信同车系候选拆解记录 -->
      <view class="section" v-if="candidates && candidates.length > 0">
        <view class="section-title">高置信同车系候选 拆解记录</view>
        <view class="candidate-list">
          <view class="candidate-card" v-for="candidate in candidates" :key="candidate.vehicleId" @click="goToDetail(candidate.vehicleId)">
            <view class="candidate-header">
              <view class="c-title" style="display:flex; justify-content:space-between; align-items:center;">
                <text>ID: {{ candidate.vehicleId }} {{ candidate.brand }} {{ candidate.model }}</text>
                <text style="color:#07c160; font-size:13px; font-weight:normal;">查看详情 ›</text>
              </view>
              <view class="c-subtitle">{{ candidate.modelYear }}款 / {{ candidate.seriesName || '-' }}</view>
              <view class="c-tags">
                <text class="tag high">置信度: {{ candidate.score }}</text>
              </view>
            </view>
            <view class="candidate-records">
              <view v-if="!candidate.dismantleRecords || candidate.dismantleRecords.length === 0" class="empty-state small">暂无拆解记录</view>
              <view v-else class="record-list">
                <view class="record-card inner-card" v-for="item in candidate.dismantleRecords" :key="item.id">
                  <view class="record-header">
                    <text class="record-time">{{ item.createdAt }}</text>
                    <text class="record-operator">{{ item.operatorName || '-' }}</text>
                  </view>
                  <view class="record-body small-grid">
                    <view class="material-item" v-for="col in dynamicColumns" :key="col.prop">
                      <text class="m-label">{{ col.label }}</text>
                      <text class="m-value">{{ item[col.prop] || 0 }}</text>
                    </view>
                    <view class="material-item">
                      <text class="m-label">其他</text>
                      <text class="m-value">{{ item.otherWeight || 0 }}</text>
                    </view>
                  </view>
                  <view class="record-remark" v-if="getFixedPriceText(item)">
                    <text class="r-label">固定总价: </text>
                    <text class="r-text">{{ getFixedPriceText(item) }}</text>
                  </view>
                  <view class="part-details" v-if="getPartDetails(item).length > 0">
                    <view class="part-section-title">高价值部件明细</view>
                    <view class="part-grid">
                      <view class="part-row" v-for="(part, pIdx) in getPartDetails(item)" :key="pIdx">
                        <text class="p-name">{{ part.partName }}</text>
                        <text v-if="part.isPremium" class="premium-tag">个体差异</text>
                        <text class="p-count">x{{ part.count }}</text>
                        <text :class="['p-price', part.isPremium ? 'premium-price' : '']" v-if="part.totalPrice > 0">¥{{ part.totalPrice }}</text>
                        <text class="p-remark" v-if="part.remark">({{ part.remark }})</text>
                      </view>
                    </view>
                    <view class="premium-note" v-if="getPartDetails(item).some(p => p.isPremium)">注：标注“个体差异”的部件受实车状况或二手件渠道影响，不计入标准保底回收价参考。</view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '../../utils/request';

const vehicleId = ref(null);
const vehicle = ref(null);
const loading = ref(true);
const records = ref([]);
const candidates = ref([]);
const dynamicColumns = ref([]);

const typeLabelMap = {
  'steel': '废钢',
  'aluminum': '废铝',
  'copper': '废铜',
  'battery': '电池',
  'plastic': '塑料',
  'rubber': '橡胶'
};

const parseDetails = (detailsJson) => {
  if (!detailsJson) return [];
  try {
    const parsed = JSON.parse(detailsJson);
    const items = Array.isArray(parsed) ? parsed : parsed.items;
    return Array.isArray(items) ? items : [];
  } catch (e) {
    return [];
  }
};

const getPartDetails = (record) => {
  const details = parseDetails(record.detailsJson);
  return details.filter(d => d.category === 'PART');
};

const getFixedPriceText = (record) => {
  const details = parseDetails(record.detailsJson);
  return details
    .filter(d => d.pricingMode === 'FIXED_TOTAL' && d.category !== 'PART' && Number(d.totalPrice) > 0)
    .map(d => `${typeLabelMap[d.materialType] || d.materialType}:${Number(d.totalPrice).toFixed(2)}元`)
    .join('，');
};

const hydrateRecordWeights = (record) => {
  const details = parseDetails(record.detailsJson);
  details
    .filter(d => d.pricingMode === 'WEIGHT' && Number(d.weightKg) > 0)
    .forEach(d => {
      record[d.materialType + 'Weight'] = Number(d.weightKg);
    });
  return record;
};

onLoad((options) => {
  vehicleId.value = options.vehicleId;
  if (vehicleId.value) {
    initData();
  }
});

const goToDetail = (id) => {
  uni.navigateTo({ url: `/pages/vehicle/detail/detail?id=${id}&mode=dismantle_records` });
};

const initData = async () => {
  loading.value = true;
  try {
    // 1. 加载车辆基本信息
    vehicle.value = await request({ url: '/vehicles/' + vehicleId.value });
    
    // 2. 加载材料类型用于动态列
    try {
      const types = await request({ url: '/admin/recycle-prices/types' });
      if (types && types.length > 0) {
        dynamicColumns.value = types.map(t => ({
          prop: t + 'Weight',
          label: typeLabelMap[t] || t
        }));
      }
    } catch (e) {
      // ignore
    }
    
    // 3. 加载当前车辆拆解记录
    const ownRecords = await request({ url: '/admin/vehicle-dismantle/vehicle/' + vehicleId.value }) || [];
    records.value = ownRecords.map(hydrateRecordWeights);
    
    // 4. 加载同车系并筛选高置信
    const sameSeriesRes = await request({ url: '/vehicles/' + vehicleId.value + '/same-series' });
    if (sameSeriesRes && sameSeriesRes.candidates) {
      const highCandidates = sameSeriesRes.candidates.filter(c => c.confidenceLevel === 'HIGH');
      // 5. 加载每个高置信候选的拆解记录
      for (let i = 0; i < highCandidates.length; i++) {
        const c = highCandidates[i];
        try {
          const candidateRecords = await request({ url: '/admin/vehicle-dismantle/vehicle/' + c.vehicleId }) || [];
          c.dismantleRecords = candidateRecords.map(hydrateRecordWeights);
        } catch (e) {
          c.dismantleRecords = [];
        }
      }
      candidates.value = highCandidates;
    }
  } catch (err) {
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};
</script>

<style>
.container { background-color: #f5f5f5; min-height: 100vh; padding-bottom: 40px; }
.header { background-color: #07c160; padding: 30px 20px; color: #fff; }
.title { font-size: 20px; font-weight: bold; margin-bottom: 5px; }
.subtitle { font-size: 14px; opacity: 0.8; }

.loading-state { text-align: center; padding: 40px; color: #999; font-size: 14px; }
.empty-state { text-align: center; padding: 30px; color: #999; font-size: 14px; background: #fff; border-radius: 8px; }
.empty-state.small { padding: 15px; background: transparent; }

.section { margin: 15px; }
.section-title { font-size: 16px; font-weight: bold; color: #333; margin-bottom: 12px; padding-left: 8px; border-left: 4px solid #07c160; }

.record-list { display: flex; flex-direction: column; gap: 12px; }
.record-card { background: #fff; border-radius: 8px; padding: 15px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.record-card.inner-card { background: #f9f9f9; box-shadow: none; border: 1px solid #eee; padding: 12px; }

.record-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f0f0f0; padding-bottom: 10px; margin-bottom: 10px; }
.record-time { font-size: 13px; color: #666; }
.record-operator { font-size: 13px; color: #333; font-weight: 500; }

.record-body { display: flex; flex-wrap: wrap; gap: 10px; }
.record-body.small-grid .material-item { width: calc(33.33% - 10px); }
.material-item { width: calc(50% - 10px); display: flex; flex-direction: column; background: #fdfdfd; padding: 8px; border-radius: 4px; border: 1px solid #f5f5f5; }
.m-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.m-value { font-size: 14px; font-weight: bold; color: #333; }

.record-remark { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #f0f0f0; font-size: 13px; }
.r-label { color: #999; }
.r-text { color: #333; }

.candidate-list { display: flex; flex-direction: column; gap: 15px; }
.candidate-card { background: #fff; border-radius: 8px; padding: 15px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.candidate-header { margin-bottom: 12px; }
.c-title { font-size: 15px; font-weight: bold; color: #333; margin-bottom: 4px; }
.c-subtitle { font-size: 13px; color: #666; margin-bottom: 8px; }
.tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.tag.high { background: #e8f9f0; color: #07c160; }

.part-details { margin-top: 10px; padding: 8px 12px; background: #f6ffed; border-radius: 6px; border: 1px solid #b7eb8f; }
.part-section-title { font-size: 12px; color: #389e0d; font-weight: bold; margin-bottom: 8px; }
.part-grid { display: flex; flex-direction: column; gap: 6px; }
.part-row { font-size: 13px; color: #333; display: flex; align-items: center; gap: 8px; }
.p-name { font-weight: bold; min-width: 60px; }
.p-count { color: #666; font-size: 12px; background: #e6f7ff; padding: 1px 6px; border-radius: 10px; }
.p-price { color: #cf1322; font-weight: 500; }
.p-remark { color: #8c8c8c; font-size: 12px; margin-left: auto; }

.premium-tag { font-size: 10px; color: #ff9800; background: #fff3e0; padding: 1px 4px; border-radius: 2px; margin-left: 4px; }
.premium-price { color: #ff9800; }
.premium-note { font-size: 11px; color: #ff9800; margin-top: 8px; border-top: 1px dashed #ffe0b2; padding-top: 6px; }
</style>
