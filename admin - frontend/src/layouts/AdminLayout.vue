<template>
  <el-container class="admin-shell">
    <el-aside width="252px" class="sidebar">
      <div class="brand"><div class="brand-icon"><el-icon><DataAnalysis /></el-icon></div><div><strong>云脑诊疗</strong><span>ADMIN CONTROL CENTER</span></div></div>
      <div class="role-label">医院运营管理</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard"><el-icon><Odometer /></el-icon><span>数据概览</span></el-menu-item>
        <el-menu-item index="/departments"><el-icon><OfficeBuilding /></el-icon><span>科室管理</span></el-menu-item>
        <el-menu-item index="/doctors"><el-icon><UserFilled /></el-icon><span>医生管理</span></el-menu-item>
        <el-menu-item index="/medicines"><el-icon><FirstAidKit /></el-icon><span>药品管理</span></el-menu-item>
        <div class="role-label mini">AI智能管理</div>
        <el-menu-item index="/ai-center"><el-icon><Monitor /></el-icon><span>AI运营中心</span></el-menu-item>
        <el-menu-item index="/ai-knowledge"><el-icon><Notebook /></el-icon><span>AI知识库</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><Setting /></el-icon><span>个人中心</span></el-menu-item>
      </el-menu>
      <div class="system-card"><div class="pulse"></div><div><strong>系统服务正常</strong><span>传统业务与 AI 服务统一接入</span></div></div>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div><h2>{{ route.meta.title || '管理中心' }}</h2><p>云脑诊疗管理中心 <span>/</span> {{ route.meta.title }}</p></div>
        <div class="top-right">
          <div class="today"><el-icon><Calendar /></el-icon><div><span>{{ weekday }}</span><strong>{{ dateText }}</strong></div></div>
          <div class="divider"></div>
          <div class="admin-user"><div class="avatar">{{ initial }}</div><div><strong>{{ auth.admin?.name || '系统管理员' }}</strong><span>平台超级管理员</span></div></div>
          <el-dropdown trigger="click"><button class="drop-btn"><el-icon><ArrowDown /></el-icon></button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="router.push('/profile')"><el-icon><Setting /></el-icon>账户设置</el-dropdown-item><el-dropdown-item divided @click="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
        </div>
      </el-header>
      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute(); const router = useRouter(); const auth = useAuthStore()
const now = new Date()
const weekday = now.toLocaleDateString('zh-CN', { weekday: 'long' })
const dateText = now.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
const initial = computed(() => (auth.admin?.name || '管').slice(0, 1))
onMounted(() => { if (!auth.admin) auth.loadInfo() })
const logout = async () => { await ElMessageBox.confirm('确定退出管理中心吗？', '退出登录', { type: 'warning' }); auth.logout(); router.push('/login') }
</script>

<style scoped>
.admin-shell{height:100vh;background:#f2f6f8}.sidebar{position:relative;display:flex;flex-direction:column;overflow:hidden;background:linear-gradient(180deg,#073b5c 0%,#075969 58%,#087a78);box-shadow:11px 0 36px rgba(17,58,75,.12)}.sidebar:after{content:"";position:absolute;right:-120px;bottom:110px;width:260px;height:260px;border:1px solid rgba(255,255,255,.08);border-radius:50%;box-shadow:0 0 0 48px rgba(255,255,255,.025)}.brand{position:relative;z-index:1;display:flex;align-items:center;gap:13px;padding:27px 23px 24px;border-bottom:1px solid rgba(255,255,255,.11)}.brand-icon{width:44px;height:44px;display:flex;align-items:center;justify-content:center;border-radius:14px;background:linear-gradient(145deg,#48d6c0,#eafffb);color:#075e68;font-size:22px;box-shadow:0 8px 20px rgba(0,0,0,.13)}.brand strong,.brand span{display:block}.brand strong{color:#fff;font-size:19px}.brand span{margin-top:3px;color:rgba(255,255,255,.5);font-size:8px;letter-spacing:1.4px}.role-label{padding:22px 25px 9px;color:rgba(255,255,255,.42);font-size:10px;font-weight:750;letter-spacing:2px}.role-label.mini{padding-top:15px}.el-menu{position:relative;z-index:1;border:0;background:transparent}.el-menu-item{height:50px;margin:5px 14px;padding:0 16px!important;border-radius:13px;color:rgba(255,255,255,.68)}.el-menu-item:hover{background:rgba(255,255,255,.08);color:#fff}.el-menu-item.is-active{background:linear-gradient(100deg,rgba(51,205,181,.28),rgba(255,255,255,.1));color:#fff;font-weight:700;box-shadow:inset 3px 0 #53dac5}.el-menu-item .el-icon{font-size:18px}.system-card{position:relative;z-index:1;display:flex;align-items:center;gap:11px;margin:auto 16px 20px;padding:15px;border:1px solid rgba(255,255,255,.11);border-radius:15px;background:rgba(255,255,255,.07);color:#fff}.system-card strong,.system-card span{display:block}.system-card strong{font-size:11px}.system-card span{margin-top:3px;color:rgba(255,255,255,.47);font-size:9px}.pulse{width:10px;height:10px;border-radius:50%;background:#47dda6;box-shadow:0 0 0 5px rgba(71,221,166,.13)}.topbar{height:76px;display:flex;align-items:center;justify-content:space-between;padding:0 30px;border-bottom:1px solid #e7eef0;background:rgba(255,255,255,.96)}.topbar h2{font-size:20px}.topbar p{margin-top:4px;color:#a0adb2;font-size:10px}.topbar p span{margin:0 5px}.top-right,.today,.admin-user{display:flex;align-items:center}.top-right{gap:16px}.today{gap:9px;color:#70868e}.today>.el-icon{color:#118d82;font-size:19px}.today span,.today strong,.admin-user strong,.admin-user span{display:block}.today span{font-size:9px}.today strong{margin-top:2px;color:#3e5963;font-size:12px}.divider{width:1px;height:31px;background:#e5edef}.admin-user{gap:10px}.avatar{width:39px;height:39px;display:flex;align-items:center;justify-content:center;border-radius:12px;background:linear-gradient(145deg,#d9f1ed,#b7dfda);color:#087f84;font-weight:800}.admin-user strong{font-size:12px}.admin-user span{margin-top:2px;color:#98a7ac;font-size:9px}.drop-btn{width:31px;height:31px;border:0;border-radius:9px;background:#eff5f6;color:#6f868d;cursor:pointer}.main{overflow-y:auto;padding:24px 27px 38px;background:radial-gradient(circle at 96% 0,#e5f4f1 0,transparent 25%),#f2f6f8}
</style>