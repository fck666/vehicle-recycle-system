<template>
  <view class="container">
    <view class="header">
      <view class="title">录入拆解数据</view>
      <view class="subtitle">{{ vehicle.brand }} {{ vehicle.model }}</view>
    </view>

    <!-- 同车系候选拆解记录 -->
    <view class="section" v-if="candidates && candidates.length > 0">
      <view class="section-title" style="font-size:14px; font-weight:bold; margin: 15px 15px 5px 15px; color:#333;">同车系录入参考</view>
      <scroll-view scroll-x class="candidate-scroll" style="white-space: nowrap; padding: 0 15px;">
        <view class="candidate-card-mini" v-for="candidate in candidates" :key="candidate.vehicleId" @click="goToCandidate(candidate.vehicleId)" style="display:inline-block; width:260px; background:#fff; border-radius:8px; padding:12px; margin-right:10px; box-shadow:0 2px 8px rgba(0,0,0,0.04); vertical-align: top; white-space: normal;">
          <view class="c-title" style="font-size:13px; font-weight:bold; color:#333; margin-bottom:4px; display:flex; justify-content:space-between;">
            <text class="ellipsis">ID: {{ candidate.vehicleId }} {{ candidate.model }}</text>
            <text style="color:#07c160; font-size:12px; flex-shrink:0; margin-left:8px;">查看/录入 ›</text>
          </view>
          <view style="font-size:12px; color:#666; margin-bottom:8px;">{{ candidate.modelYear }}款 / {{ candidate.seriesName || '-' }}</view>
          
          <view v-if="candidate.dismantleRecords && candidate.dismantleRecords.length > 0" style="background:#f9f9f9; padding:6px; border-radius:4px; font-size:11px;">
            <view style="color:#999; margin-bottom:2px;">最新录入: {{ candidate.dismantleRecords[0].createdAt }}</view>
            <view style="color:#333; display:flex; flex-wrap:wrap; gap:4px;">
              <text v-for="col in dynamicItems" :key="col.prop" v-show="candidate.dismantleRecords[0][col.type + 'Weight']">
                {{ col.label }}: {{ candidate.dismantleRecords[0][col.type + 'Weight'] }}
              </text>
              <text v-show="candidate.dismantleRecords[0].otherWeight">其他: {{ candidate.dismantleRecords[0].otherWeight }}</text>
            </view>
          </view>
          <view v-else style="font-size:11px; color:#999; text-align:center; padding:10px 0; background:#f9f9f9; border-radius:4px;">
            暂无拆解记录
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="form-card">
      <view class="mode-switch">
        <text class="label">录入模式</text>
        <view class="switch-group">
          <view :class="['switch-item', mode === 'weight' ? 'active' : '']" @click="mode = 'weight'">按重量</view>
          <view :class="['switch-item', mode === 'ratio' ? 'active' : '']" @click="mode = 'ratio'">按比例</view>
        </view>
      </view>
      
      <view v-if="mode === 'ratio'" class="curb-weight-info">
        整备质量: {{ vehicle.curbWeight ? vehicle.curbWeight + ' kg' : '未知' }}
      </view>

      <view v-for="(item, index) in dynamicItems" :key="index" class="form-item">
        <text class="label">{{ item.label }} ({{ mode === 'weight' ? 'kg' : '%' }})</text>
        <view class="input-wrapper">
          <input 
            class="input" 
            type="digit" 
            v-model="item.value" 
            :placeholder="mode === 'weight' ? '输入重量' : '输入比例'"
            :disabled="mode === 'ratio' && !vehicle.curbWeight"
          />
          <text v-if="mode === 'ratio' && vehicle.curbWeight" class="calc-hint">
            ≈ {{ ((Number(item.value) / 100) * vehicle.curbWeight).toFixed(1) }} kg
          </text>
        </view>
      </view>

      <view v-for="(item, index) in fixedItems" :key="'fixed-' + index" class="form-item">
        <text class="label">{{ item.label }} (元)</text>
        <view class="input-wrapper">
          <input class="input" type="digit" v-model="item.totalPrice" placeholder="输入总价" />
        </view>
      </view>

      <view class="form-item">
        <text class="label">其他材料 (kg)</text>
        <input class="input" type="digit" v-model="formOther" placeholder="请输入重量" />
      </view>
      
      <view class="form-item full-width">
        <text class="label">备注信息</text>
        <textarea class="textarea" v-model="formRemark" placeholder="请输入备注内容" />
      </view>
    </view>

    <!-- 高价值部件录入 -->
    <view class="form-card" style="margin-top: 15px;">
      <view class="section-title" style="font-size:14px; font-weight:bold; margin-bottom: 10px; color:#333;">高价值部件录入</view>
      
      <view v-for="(part, index) in partItems" :key="index" class="part-item-box" style="background: #f9f9f9; padding: 10px; border-radius: 6px; margin-bottom: 10px;">
        <view style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 8px;">
          <text style="font-size:13px; font-weight:bold;">部件 {{ index + 1 }}</text>
          <text style="color:#ff4d4f; font-size:12px;" @click="removePart(index)">删除</text>
        </view>
        
        <view class="form-item" style="padding: 0; margin-bottom: 8px; border:none;">
          <text class="label" style="width:60px;">名称</text>
          <picker mode="selector" :range="PART_OPTIONS" @change="e => part.partName = PART_OPTIONS[e.detail.value]" style="flex:1;">
            <view class="input" style="height:36px; line-height:36px; background:#fff; border:1px solid #eee; padding:0 10px;">
              {{ part.partName || '请选择部件名称' }}
            </view>
          </picker>
        </view>

        <view style="display:flex; gap:10px;">
          <view class="form-item" style="padding: 0; margin-bottom: 8px; border:none; flex:1;">
            <text class="label" style="width:40px;">数量</text>
            <input class="input" type="number" v-model="part.count" style="background:#fff; border:1px solid #eee; height:36px;" />
          </view>
          <view class="form-item" style="padding: 0; margin-bottom: 8px; border:none; flex:1;">
            <text class="label" style="width:40px;">总价</text>
            <input class="input" type="digit" v-model="part.totalPrice" placeholder="元" style="background:#fff; border:1px solid #eee; height:36px;" />
          </view>
        </view>
        
        <view style="display:flex; align-items:center; gap:10px; margin-bottom: 8px;">
          <text style="font-size:13px; color:#666; width:60px;">个体差异</text>
          <switch :checked="part.isPremium" @change="e => part.isPremium = e.detail.value" color="#ff9800" style="transform:scale(0.8); margin-left:-10px;"/>
          <text style="font-size:11px; color:#999; flex:1;">(如成色极好/二手件导致的高溢价，非标准底价)</text>
        </view>

        <view class="form-item" style="padding: 0; border:none;">
          <text class="label" style="width:60px;">去向/备注</text>
          <input class="input" type="text" v-model="part.remark" placeholder="如:走二手件/改装音响" style="background:#fff; border:1px solid #eee; height:36px;" />
        </view>
      </view>

      <view style="text-align:center; padding: 10px 0;">
        <button type="default" size="mini" @click="addPart" style="color:#07c160; border-color:#07c160; background:#fff;">+ 添加部件</button>
      </view>
    </view>

    <view class="submit-bar">
      <button class="submit-btn" type="primary" @click="handleSubmit">提交记录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '../../utils/request';

const vehicleId = ref(null);
const recordId = ref('');
const vehicle = ref({});
const mode = ref('weight');
const dynamicItems = ref([]);
const fixedItems = ref([]);
const formOther = ref('');
const formRemark = ref('');
const candidates = ref([]);

// 部件录入相关
const PART_OPTIONS = ref([]);
const partItems = ref([]);

const addPart = () => {
  partItems.value.push({
    category: 'PART',
    partName: '',
    count: 1,
    totalPrice: '',
    pricingMode: 'FIXED_TOTAL',
    isPremium: false,
    remark: ''
  });
};

const removePart = (index) => {
  partItems.value.splice(index, 1);
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

const hydrateRecordWeights = (record) => {
  const details = parseDetails(record.detailsJson);
  details
    .filter(d => d.pricingMode === 'WEIGHT' && Number(d.weightKg) > 0)
    .forEach(d => {
      record[d.materialType + 'Weight'] = Number(d.weightKg);
    });
  return record;
};

// Mapping for standard types
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
  if (options.recordId) {
    recordId.value = options.recordId;
  }
  loadVehicle();
  loadRecycleTypes();
  loadSameSeries();
  loadComponents();
});

const loadComponents = async () => {
  try {
    const components = await request({ url: '/components' }) || [];
    if (components && components.length > 0) {
      PART_OPTIONS.value = components.map(c => c.name);
    } else {
      // 兜底数据
      PART_OPTIONS.value = ['三元催化', '发动机', '变速箱', '轮毂', '电机', '空调压缩机', '发电机', '音响', '中控', '座椅', '电瓶'];
    }
  } catch (e) {
    PART_OPTIONS.value = ['三元催化', '发动机', '变速箱', '轮毂', '电机', '空调压缩机', '发电机', '音响', '中控', '座椅', '电瓶'];
  }

  // 6. 如果有 recordId，加载记录并回显
  if (recordId.value) {
    try {
      const recordRes = await request({ url: '/admin/vehicle-dismantle/' + recordId.value });
      if (recordRes) {
        // Fill main weights
        dynamicItems.value.forEach(item => {
          if (item.type === 'steel') item.value = recordRes.steelWeight || '';
          else if (item.type === 'aluminum') item.value = recordRes.aluminumWeight || '';
          else if (item.type === 'copper') item.value = recordRes.copperWeight || '';
          else if (item.type === 'battery') item.value = recordRes.batteryWeight || '';
        });
        formOther.value = recordRes.otherWeight || '';
        formRemark.value = recordRes.remark || '';
        
        // Fill details (parts and other materials)
        if (recordRes.detailsJson) {
          const parsed = JSON.parse(recordRes.detailsJson);
          const items = parsed.items || [];
          
          // Extract materials
          items.filter(i => i.category === 'MATERIAL').forEach(mat => {
            const dynItem = dynamicItems.value.find(d => d.type === mat.materialType);
            if (dynItem) {
              dynItem.value = mode.value === 'ratio' ? mat.ratio : mat.weightKg;
            }
            const fixItem = fixedItems.value.find(f => f.type === mat.materialType);
            if (fixItem) {
              fixItem.totalPrice = mat.totalPrice;
            }
          });

          // Extract parts
          partItems.value = items.filter(i => i.category === 'PART').map(p => ({
            category: 'PART',
            partName: p.partName || '',
            count: p.count || 1,
            totalPrice: p.totalPrice || '',
            pricingMode: 'FIXED_TOTAL',
            isPremium: p.isPremium || false,
            remark: p.remark || ''
          }));
        }
      }
    } catch (e) {
      uni.showToast({ title: '加载记录失败', icon: 'none' });
    }
  }
};

const loadSameSeries = async () => {
  try {
    const res = await request({ url: '/vehicles/' + vehicleId.value + '/same-series' });
    if (res && res.candidates) {
      const highCandidates = res.candidates.filter(c => c.confidenceLevel === 'HIGH');
      for (let i = 0; i < highCandidates.length; i++) {
        const c = highCandidates[i];
        try {
          const records = await request({ url: '/admin/vehicle-dismantle/vehicle/' + c.vehicleId }) || [];
          c.dismantleRecords = records.map(hydrateRecordWeights);
        } catch (e) {
          c.dismantleRecords = [];
        }
      }
      candidates.value = highCandidates;
    }
  } catch (e) {
    console.error('Failed to load same series', e);
  }
};

const goToCandidate = (id) => {
  uni.navigateTo({ url: `/pages/dismantle/entry?vehicleId=${id}` });
};

const loadVehicle = () => {
  request({ url: '/vehicles/' + vehicleId.value }).then(res => {
    vehicle.value = res || {};
  });
};

const loadRecycleTypes = () => {
  request({ url: '/admin/recycle-prices/types' }).then((typesRes) => {
    const types = Array.isArray(typesRes) ? typesRes : [];
    dynamicItems.value = types.map(t => ({
      type: t,
      label: typeLabelMap[t] || t,
      value: ''
    }));
    fixedItems.value = [];
  }).catch(() => {
    dynamicItems.value = [];
    fixedItems.value = [];
  });
};

const handleSubmit = () => {
  // Check if at least one input has value
  const hasValue =
    dynamicItems.value.some(item => Number(item.value) > 0) ||
    fixedItems.value.some(item => Number(item.totalPrice) > 0) ||
    Number(formOther.value) > 0 ||
    partItems.value.length > 0;
  
  if (!hasValue) {
    uni.showToast({ title: '请至少输入一项数值或添加一个部件', icon: 'none' });
    return;
  }

  // Validate part items
  for (let i = 0; i < partItems.value.length; i++) {
    if (!partItems.value[i].partName) {
      uni.showToast({ title: `请选择部件 ${i + 1} 的名称`, icon: 'none' });
      return;
    }
  }

  uni.showLoading({ title: '提交中...' });
  
  let steel = 0, aluminum = 0, copper = 0, battery = 0;
  const detailItems = [];
  
  dynamicItems.value.forEach(item => {
    let val = Number(item.value) || 0;
    if (mode.value === 'ratio' && vehicle.value.curbWeight) {
      val = Number(((val / 100) * vehicle.value.curbWeight).toFixed(2));
    }
    
    if (item.type === 'steel') steel = val;
    else if (item.type === 'aluminum') aluminum = val;
    else if (item.type === 'copper') copper = val;
    else if (item.type === 'battery') battery = val;
    else if (val > 0) {
      detailItems.push({
        category: 'MATERIAL',
        materialType: item.type,
        pricingMode: 'WEIGHT',
        weightKg: val,
        ratio: mode.value === 'ratio' ? Number(item.value) || 0 : null
      });
    }
  });
  fixedItems.value.forEach(item => {
    const totalPrice = Number(item.totalPrice) || 0;
    if (totalPrice > 0) {
      detailItems.push({
        category: 'MATERIAL',
        materialType: item.type,
        pricingMode: 'FIXED_TOTAL',
        totalPrice
      });
    }
  });

  // Append part items
  partItems.value.forEach(part => {
    detailItems.push({
      category: 'PART',
      partName: part.partName,
      count: Number(part.count) || 1,
      totalPrice: Number(part.totalPrice) || 0,
      pricingMode: 'FIXED_TOTAL',
      isPremium: part.isPremium || false,
      remark: part.remark
    });
  });

  const data = {
    vehicleId: Number(vehicleId.value),
    steelWeight: steel,
    aluminumWeight: aluminum,
    copperWeight: copper,
    batteryWeight: battery,
    otherWeight: Number(formOther.value) || 0,
    detailsJson: JSON.stringify({ items: detailItems }),
    remark: formRemark.value
  };

  const url = recordId.value 
    ? `/admin/vehicle-dismantle/${recordId.value}`
    : '/admin/vehicle-dismantle';

  const method = recordId.value ? 'PUT' : 'POST';

  request({
    url,
    method,
    data: data
  }).then(() => {
    uni.showToast({ title: '提交成功' });
    setTimeout(() => {
      uni.navigateBack();
    }, 1500);
  }).catch(err => {
    uni.showToast({ title: '提交失败: ' + (err.message || '权限不足'), icon: 'none' });
  }).finally(() => {
    uni.hideLoading();
  });
};
</script>

<style>
.container { background-color: #f5f5f5; min-height: 100vh; padding-bottom: 80px; }
.header { background-color: #07c160; padding: 30px 20px; color: #fff; }
.title { font-size: 20px; font-weight: bold; margin-bottom: 5px; }
.subtitle { font-size: 14px; opacity: 0.8; }

.form-card { background-color: #fff; margin: 15px; border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 15px; }
.form-item { border-bottom: 1px solid #f0f0f0; padding-bottom: 10px; }
.label { font-size: 14px; color: #666; margin-bottom: 8px; display: block; }
.input-wrapper { display: flex; align-items: center; justify-content: space-between; }
.input { height: 40px; font-size: 16px; color: #333; flex: 1; }
.calc-hint { font-size: 12px; color: #999; margin-left: 10px; }
.textarea { width: 100%; height: 80px; font-size: 14px; padding-top: 10px; border: 1px solid #eee; border-radius: 4px; padding: 8px; box-sizing: border-box; }

.mode-switch { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.switch-group { display: flex; border: 1px solid #07c160; border-radius: 4px; overflow: hidden; }
.switch-item { padding: 4px 12px; font-size: 12px; color: #07c160; background: #fff; }
.switch-item.active { background: #07c160; color: #fff; }
.curb-weight-info { font-size: 12px; color: #f0ad4e; margin-bottom: 10px; background: #fdf6ec; padding: 8px; border-radius: 4px; }

.submit-bar { position: fixed; bottom: 0; left: 0; right: 0; background-color: #fff; padding: 15px 20px; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); z-index: 100; }
.submit-btn { border-radius: 25px; font-size: 16px; height: 50px; line-height: 50px; background-color: #07c160; }
.ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
