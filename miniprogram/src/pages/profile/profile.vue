<template>
  <view class="container">
    <view class="user-section" @click="onHeaderTap">
      <view class="avatar-box">
        <image class="avatar" src="/static/logo.png"></image>
      </view>
      <view class="info-box">
        <view class="username-row">
          <text class="username">{{ displayName }}</text>
          <text class="edit-icon" v-if="isLoggedIn">✎</text>
        </view>
        <text class="role-tag" v-if="isLoggedIn">{{ isStaff ? '工作人员' : '普通用户' }}</text>
      </view>
    </view>

    <view class="list-section">
      <view class="list-item" v-if="showBindItem" @click="onBindTap">
        <text class="list-text">绑定员工账号</text>
        <text class="arrow">›</text>
      </view>
      <view class="list-item" v-if="showLogoutItem" @click="onLogoutTap">
        <text class="list-text">退出登录</text>
        <text class="arrow">›</text>
      </view>
      <view class="list-item" @click="onAboutTap">
        <text class="list-text">关于系统</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="modal" v-if="showEditModal">
      <view class="modal-content">
        <view class="modal-title">修改用户名</view>
        <input class="input" v-model="newUsername" placeholder="请输入新用户名" maxlength="20" />
        <view class="modal-btns">
          <button size="mini" @click="onCancelTap">取消</button>
          <button size="mini" type="primary" @click="onSaveTap">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import request from '../../utils/request';

export default {
  data() {
    return {
      isLoggedIn: false,
      isStaff: false,
      username: '未登录',
      showEditModal: false,
      newUsername: '',
      displayName: '点击登录',
      showBindItem: false,
      showLogoutItem: false
    }
  },
  onShow() {
    this.checkLoginStatus()
  },
  methods: {
    checkLoginStatus() {
      const token = uni.getStorageSync('token')
      if (!token) {
        this.isLoggedIn = false
        this.isStaff = false
        this.username = '未登录'
        this.displayName = '点击登录'
        this.showBindItem = false
        this.showLogoutItem = false
        return
      }
      this.isLoggedIn = true
      const roles = uni.getStorageSync('roles') || []
      this.isStaff = roles.includes('ADMIN') || roles.includes('OPERATOR')
      const cachedUsername = uni.getStorageSync('username')
      this.username = cachedUsername || '微信用户'
      this.displayName = this.isStaff ? `员工: ${this.username}` : this.username
      this.newUsername = this.username
      this.showBindItem = !this.isStaff
      this.showLogoutItem = true
    },
    onHeaderTap() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      this.newUsername = this.username
      this.showEditModal = true
    },
    onCancelTap() {
      this.showEditModal = false
    },
    onSaveTap() {
      const nextName = (this.newUsername || '').trim()
      if (!nextName) {
        uni.showToast({ title: '用户名不能为空', icon: 'none' })
        return
      }
      if (nextName === this.username) {
        this.showEditModal = false
        return
      }
      uni.showLoading({ title: '保存中...' })
      request({
        url: '/auth/me/username',
        method: 'PUT',
        data: { username: nextName }
      }).then(() => {
        this.username = nextName
        this.displayName = this.isStaff ? `员工: ${nextName}` : nextName
        uni.setStorageSync('username', nextName)
        this.showEditModal = false
        uni.showToast({ title: '修改成功' })
      }).catch(() => {
        uni.showToast({ title: '修改失败，可能用户名已存在', icon: 'none' })
      }).finally(() => {
        uni.hideLoading()
      })
    },
    onBindTap() {
      uni.navigateTo({ url: '/pages/login/login' })
    },
    onLogoutTap() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (!res.confirm) return
          uni.clearStorageSync()
          this.isLoggedIn = false
          this.isStaff = false
          this.username = '未登录'
          this.displayName = '点击登录'
          this.showBindItem = false
          this.showLogoutItem = false
          uni.showToast({ title: '已退出' })
        }
      })
    },
    onAboutTap() {
      uni.showModal({
        title: '关于',
        content: '车辆拆解回收系统 v1.0.0',
        showCancel: false
      })
    }
  }
}
</script>

<style>
.container { background-color: #f8f8f8; min-height: 100vh; }
.user-section { background-color: #fff; padding: 30px 20px; display: flex; align-items: center; margin-bottom: 10px; }
.avatar-box { width: 60px; height: 60px; border-radius: 30px; overflow: hidden; background-color: #eee; margin-right: 15px; }
.avatar { width: 100%; height: 100%; }
.info-box { display: flex; flex-direction: column; }
.username-row { display: flex; align-items: center; margin-bottom: 5px; }
.username { font-size: 18px; font-weight: bold; color: #333; }
.edit-icon { margin-left: 8px; font-size: 16px; color: #999; padding: 2px 5px; }
.role-tag { font-size: 12px; color: #07c160; background: rgba(7, 193, 96, 0.1); padding: 2px 8px; border-radius: 10px; width: fit-content; }
.list-section { background-color: #fff; }
.list-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; border-bottom: 1px solid #f0f0f0; }
.list-text { font-size: 16px; color: #333; }
.arrow { color: #ccc; font-size: 16px; }
.modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-content { background: #fff; width: 80%; padding: 20px; border-radius: 10px; }
.modal-title { text-align: center; font-size: 18px; margin-bottom: 20px; font-weight: bold; }
.input { border: 1px solid #ddd; height: 40px; border-radius: 4px; padding: 0 10px; margin-bottom: 15px; width: 100%; box-sizing: border-box; }
.modal-btns { display: flex; justify-content: space-around; margin-top: 20px; }
</style>
