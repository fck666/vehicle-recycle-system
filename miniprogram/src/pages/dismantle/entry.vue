<template>
  <view class="container">
    <view class="header">
      <view class="title">录入拆解数据</view>
      <view class="subtitle">{{ vehicle.brand }} {{ vehicle.model }}</view>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="label">废钢重量 (kg)</text>
        <input class="input" type="digit" v-model="form.steelWeight" placeholder="请输入重量" />
      </view>
      <view class="form-item">
        <text class="label">废铝重量 (kg)</text>
        <input class="input" type="digit" v-model="form.aluminumWeight" placeholder="请输入重量" />
      </view>
      <view class="form-item">
        <text class="label">废铜重量 (kg)</text>
        <input class="input" type="digit" v-model="form.copperWeight" placeholder="请输入重量" />
      </view>
      <view class="form-item">
        <text class="label">电池重量 (kg)</text>
        <input class="input" type="digit" v-model="form.batteryWeight" placeholder="请输入重量" />
      </view>
      <view class="form-item">
        <text class="label">其他材料 (kg)</text>
        <input class="input" type="digit" v-model="form.otherWeight" placeholder="请输入重量" />
      </view>
      <view class="form-item full-width">
        <text class="label">备注信息</text>
        <textarea class="textarea" v-model="form.remark" placeholder="请输入备注内容" />
      </view>
    </view>

    <view class="submit-bar">
      <button class="submit-btn" type="primary" @click="handleSubmit">提交记录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '../../utils/request';

const vehicleId = ref(null);
const vehicle = ref({});
const form = reactive({
  steelWeight: '',
  aluminumWeight: '',
  copperWeight: '',
  batteryWeight: '',
  otherWeight: '',
  remark: ''
});

onLoad((options) => {
  vehicleId.value = options.vehicleId;
  loadVehicle();
});

const loadVehicle = () => {
  request({ url: '/vehicles/' + vehicleId.value }).then(res => {
    vehicle.value = res;
  });
};

const handleSubmit = () => {
  if (!form.steelWeight && !form.aluminumWeight && !form.copperWeight && !form.batteryWeight) {
    uni.showToast({ title: '请至少输入一项重量', icon: 'none' });
    return;
  }

  uni.showLoading({ title: '提交中...' });
  
  // 转换数据类型
  const data = {
    vehicleId: Number(vehicleId.value),
    steelWeight: Number(form.steelWeight) || 0,
    aluminumWeight: Number(form.aluminumWeight) || 0,
    copperWeight: Number(form.copperWeight) || 0,
    batteryWeight: Number(form.batteryWeight) || 0,
    otherWeight: Number(form.otherWeight) || 0,
    remark: form.remark
  };

  request({
    url: '/admin/vehicle-dismantle',
    method: 'POST',
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

.form-card { background-color: #fff; margin: 15px; border-radius: 12px; padding: 20px; display: flex; flex-wrap: wrap; }
.form-item { width: 100%; margin-bottom: 20px; border-bottom: 1px solid #f0f0f0; padding-bottom: 10px; }
.label { font-size: 14px; color: #666; margin-bottom: 8px; display: block; }
.input { height: 40px; font-size: 16px; color: #333; }
.textarea { width: 100%; height: 80px; font-size: 14px; padding-top: 10px; }

.submit-bar { position: fixed; bottom: 0; left: 0; right: 0; background-color: #fff; padding: 15px 20px; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); }
.submit-btn { border-radius: 25px; font-size: 16px; height: 50px; line-height: 50px; }
</style>
