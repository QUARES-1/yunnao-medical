<template>
  <view class="page">
    <view class="profile">
      <image v-if="avatarUrl" class="avatar avatar-image" :src="avatarUrl" mode="aspectFill" />
      <view v-else class="avatar">{{ auth.patient?.name?.slice(0, 1) || '我' }}</view>
      <view class="profile-info"><text class="name">{{ auth.patient?.name || '未登录用户' }}</text><text>{{ auth.patient?.phone || '登录后管理健康档案' }}</text></view>
      <button v-if="!auth.isLoggedIn()" :loading="auth.loading" @tap="login">微信登录</button>
      <view v-else class="edit-entry" @tap="go('/pages/profile/edit')">编辑 ›</view>
    </view>
    <view class="card">
      <view @tap="go('/pages/registrations/list')"><text>挂号记录</text><text>查看全部记录 ›</text></view>
      <view><text>电子病历</text><text>即将开放 ›</text></view><view><text>检查报告</text><text>即将开放 ›</text></view><view><text>我的处方</text><text>即将开放 ›</text></view>
    </view>
    <view class="service">健康服务热线 <b>400-888-2026</b></view>
  </view>
</template>
<script setup lang="ts">
import { computed } from 'vue';import{onShow}from'@dcloudio/uni-app';import{useAuthStore}from'@/stores/auth';import{API_BASE_URL,showError}from'@/utils/request';
const auth=useAuthStore(),go=(url:string)=>uni.navigateTo({url});
const avatarUrl=computed(()=>{const avatar=auth.patient?.avatar;return avatar?(avatar.startsWith('/')?`${API_BASE_URL}${avatar}`:avatar):''});
async function login(){try{await auth.login();uni.showToast({title:'登录成功'})}catch(e){showError(e)}}
onShow(()=>auth.loadProfile().catch(()=>undefined));
</script>
<style scoped>.page{padding:32rpx 30rpx}.profile{background:linear-gradient(145deg,#075f72,#15958e);color:#fff;border-radius:34rpx;padding:36rpx;display:flex;align-items:center}.avatar{width:100rpx;height:100rpx;flex:none;border-radius:32rpx;background:rgba(255,255,255,.2);display:flex;align-items:center;justify-content:center;font-size:38rpx;margin-right:22rpx}.avatar-image{display:block;background:#d9efec}.profile-info{flex:1;min-width:0}.profile text{display:block;font-size:21rpx;opacity:.75}.profile .name{font-size:31rpx;font-weight:700;opacity:1;margin-bottom:10rpx}.profile button{margin:0;padding:0 20rpx;height:62rpx;line-height:62rpx;border-radius:20rpx;background:#fff;color:#087f84;font-size:22rpx}.edit-entry{flex:none;margin-left:14rpx;padding:14rpx 18rpx;border-radius:18rpx;background:rgba(255,255,255,.16);font-size:21rpx}.card{margin-top:28rpx;background:#fff;border-radius:28rpx;padding:0 28rpx}.card view{display:flex;justify-content:space-between;padding:30rpx 0;border-bottom:1rpx solid #edf2f3;font-size:26rpx}.card view:last-child{border:0}.card text:last-child{font-size:22rpx;color:#95a5ab}.service{text-align:center;color:#8fa1a8;font-size:21rpx;margin-top:50rpx}.service b{color:#087f84}</style>
