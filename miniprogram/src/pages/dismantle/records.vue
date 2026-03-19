<template>
  <view class="container">
    <view class="header">
      <view class="title">拆解记录</view>
      <view class="subtitle" v-if="vehicle">{{ vehicle.brand }} {{ vehicle.model }}</view>
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
    records.value = await request({ url: '/admin/vehicle-dismantle/vehicle/' + vehicleId.value }) || [];
    
    // 4. 加载同车系并筛选高置信
    const sameSeriesRes = await request({ url: '/vehicles/' + vehicleId.value + '/same-series' });
    if (sameSeriesRes && sameSeriesRes.candidates) {
      const highCandidates = sameSeriesRes.candidates.filter(c => c.confidenceLevel === 'HIGH');
      // 5. 加载每个高置信候选的拆解记录
      for (let i = 0; i < highCandidates.length; i++) {
        const c = highCandidates[i];
        try {
          c.dismantleRecords = await request({ url: '/admin/vehicle-dismantle/vehicle/' + c.vehicleId }) || [];
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
</style>
