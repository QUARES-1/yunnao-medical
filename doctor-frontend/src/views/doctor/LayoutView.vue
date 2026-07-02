<template>
  <el-container class="layout-container">
    <el-aside width="248px" class="layout-aside">
      <div class="brand">
        <div class="brand-mark"><el-icon><FirstAidKit /></el-icon></div>
        <div>
          <strong>云脑诊疗</strong>
          <span>NEUSOFT SMART CARE</span>
        </div>
      </div>
      <div class="workspace-label">医生工作站</div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/workbench">
          <el-icon><Monitor /></el-icon>
          <span>今日工作台</span>
        </el-menu-item>
        <el-menu-item index="/history">
          <el-icon><Document /></el-icon>
          <span>历史诊疗记录</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div class="secure-icon"><el-icon><Lock /></el-icon></div>
        <div>
          <strong>医疗数据安全连接</strong>
          <span>系统运行正常</span>
        </div>
        <i></i>
      </div>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <div class="page-title">{{currentTitle}}</div>
          <div class="breadcrumb">医生工作站 <span>/</span> {{currentTitle}}</div>
        </div>
        <div class="header-right">
          <!-- 通知铃铛 -->
          <div class="notification-bell" @click="showNotificationDrawer = true">
            <el-badge :value="doctorStore.unreadCount" :hidden="doctorStore.unreadCount === 0" class="notification-badge">
              <el-icon size="20"><Bell /></el-icon>
            </el-badge>
          </div>
          <div class="header-divider"></div>
          <div class="date-box">
            <el-icon><Calendar /></el-icon>
            <div>
              <span>{{weekday}}</span>
              <strong>{{today}}</strong>
            </div>
          </div>
          <div class="header-divider"></div>
          <div class="doctor-mini">
            <div class="mini-avatar">{{doctorStore.doctorInitial}}</div>
            <div>
              <strong>{{doctorStore.doctorName}}</strong>
              <span>{{doctorStore.doctorTitle}} · {{doctorStore.departmentName}}</span>
            </div>
          </div>
          <el-dropdown trigger="click">
            <button class="more-button"><el-icon><ArrowDown /></el-icon></button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 通知抽屉 -->
  <el-drawer v-model="showNotificationDrawer" title="通知中心" size="380px">
    <div v-if="doctorStore.notifications.length === 0" class="notification-empty">
      <el-empty description="暂无通知" />
    </div>
    <div v-else class="notification-list">
      <div
        v-for="(n, idx) in doctorStore.notifications"
        :key="idx"
        :class="['notification-item', { unread: !n.read }]"
        @click="doctorStore.markRead(idx)"
      >
        <div class="notification-item-header">
          <el-tag
            :type="n.type === 'HIGH_RISK_MEDICATION' ? 'danger' : 'warning'"
            size="small"
            effect="dark"
          >
            {{ n.type === 'HIGH_RISK_MEDICATION' ? '高风险' : '提醒' }}
          </el-tag>
          <span class="notification-time">{{ n.time }}</span>
        </div>
        <div class="notification-title">{{ n.title }}</div>
        <div class="notification-content">{{ n.content }}</div>
        <div v-if="n.data?.suggestions" class="notification-suggestions">
          {{ n.data.suggestions }}
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  Bell, FirstAidKit, Monitor, Document, User, Lock,
  Calendar, ArrowDown, SwitchButton
} from '@element-plus/icons-vue'
import { useDoctorStore } from '../../stores'

const route = useRoute()
const router = useRouter()
const doctorStore = useDoctorStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '工作台')

const now = new Date()
const weekday = now.toLocaleDateString('zh-CN', { weekday: 'long' })
const today = now.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })

const showNotificationDrawer = ref(false)

onMounted(() => {
  // 先从缓存快速恢复，再异步刷新
  doctorStore.restoreFromCache()
  doctorStore.fetchDoctorInfo()
  window.addEventListener('doctor-profile-updated', () => doctorStore.fetchDoctorInfo())
  connectWebSocket()
})

onUnmounted(() => {
  window.removeEventListener('doctor-profile-updated', () => doctorStore.fetchDoctorInfo())
  disconnectWebSocket()
})

const handleLogout = async () => {
  await ElMessageBox.confirm('确定退出医生工作站吗？', '退出登录', {
    type: 'warning',
    confirmButtonText: '确认退出',
    cancelButtonText: '暂不退出'
  })
  doctorStore.clearDoctorInfo()
  ElMessage.success('已安全退出')
  router.push('/login')
}

// ============ WebSocket 实时通知 ============
const ws = ref(null)
let heartbeatTimer = null
let reconnectTimer = null

const connectWebSocket = () => {
  if (ws.value?.readyState === WebSocket.OPEN) return
  const token = localStorage.getItem('token')
  const doctorId = doctorStore.doctorId
  if (!token || !doctorId) {
    reconnectTimer = setTimeout(connectWebSocket, 2000)
    return
  }

  const wsUrl = `ws://localhost:8080/ws/doctor/${doctorId}?token=${token}`
  ws.value = new WebSocket(wsUrl)

  ws.value.onopen = () => {
    console.log('[WebSocket] 连接成功')
    doctorStore.setWsConnected(true)
    startHeartbeat()
  }

  ws.value.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      handleNotification(msg)
    } catch (e) {
      console.log('[WebSocket] 收到非JSON消息:', event.data)
    }
  }

  ws.value.onclose = () => {
    console.log('[WebSocket] 连接断开，3秒后重连')
    doctorStore.setWsConnected(false)
    clearInterval(heartbeatTimer)
    reconnectTimer = setTimeout(connectWebSocket, 3000)
  }

  ws.value.onerror = (err) => {
    console.error('[WebSocket] 错误:', err)
  }
}

const disconnectWebSocket = () => {
  clearInterval(heartbeatTimer)
  clearTimeout(reconnectTimer)
  doctorStore.setWsConnected(false)
  if (ws.value) {
    ws.value.close()
    ws.value = null
  }
}

const startHeartbeat = () => {
  clearInterval(heartbeatTimer)
  heartbeatTimer = setInterval(() => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send('ping')
    }
  }, 30000)
}

const handleNotification = (msg) => {
  // 写入 store（持久化到响应式状态）
  doctorStore.addNotification(msg)

  // 自动弹窗通知
  ElNotification({
    title: msg.title || '系统通知',
    message: msg.content || '',
    type: msg.type === 'HIGH_RISK_MEDICATION' ? 'error' : 'warning',
    duration: 0,
    position: 'top-right'
  })
}
</script>

<style scoped>
.layout-container{height:100vh;background:#f3f7f9}
.layout-aside{position:relative;display:flex;flex-direction:column;overflow:hidden;background:linear-gradient(180deg,#073b5c 0%,#075866 58%,#087b78 100%);box-shadow:10px 0 35px rgba(17,58,75,.12)}
.layout-aside:after{content:'';position:absolute;right:-110px;bottom:90px;width:240px;height:240px;border:1px solid rgba(255,255,255,.08);border-radius:50%;box-shadow:0 0 0 45px rgba(255,255,255,.025)}
.brand{position:relative;z-index:1;display:flex;align-items:center;gap:13px;padding:27px 24px 24px;border-bottom:1px solid rgba(255,255,255,.11)}
.brand-mark{width:43px;height:43px;display:flex;align-items:center;justify-content:center;border-radius:14px;background:linear-gradient(145deg,#31c0ad,#e9fffb);color:#08636b;font-size:22px;box-shadow:0 8px 20px rgba(0,0,0,.13)}
.brand strong,.brand span{display:block}
.brand strong{color:#fff;font-size:19px;letter-spacing:1px}
.brand span{margin-top:3px;color:rgba(255,255,255,.5);font-size:8px;letter-spacing:1.4px}
.workspace-label{padding:23px 25px 10px;color:rgba(255,255,255,.42);font-size:10px;font-weight:700;letter-spacing:2px}
.el-menu{position:relative;z-index:1;border:0;background:transparent}
.el-menu-item{height:50px;margin:5px 14px;padding:0 16px!important;border-radius:13px;color:rgba(255,255,255,.67);transition:.25s}
.el-menu-item:hover{background:rgba(255,255,255,.08);color:#fff}
.el-menu-item.is-active{background:linear-gradient(100deg,rgba(51,205,181,.28),rgba(255,255,255,.10));color:#fff;font-weight:650;box-shadow:inset 3px 0 #53dac5}
.el-menu-item .el-icon{font-size:19px}
.aside-footer{position:relative;z-index:1;display:flex;align-items:center;gap:10px;margin:auto 16px 20px;padding:14px;border:1px solid rgba(255,255,255,.11);border-radius:15px;background:rgba(255,255,255,.07);color:#fff}
.secure-icon{width:34px;height:34px;display:flex;align-items:center;justify-content:center;border-radius:10px;background:rgba(74,219,190,.16);color:#61dbc5}
.aside-footer strong,.aside-footer span{display:block}
.aside-footer strong{font-size:11px}
.aside-footer span{font-size:9px;color:rgba(255,255,255,.55)}
.layout-header{display:flex;align-items:center;justify-content:space-between;height:64px;padding:0 28px;border-bottom:1px solid #e8eef0;background:#fff}
.header-left{display:flex;flex-direction:column;gap:2px}
.page-title{font-size:16px;font-weight:700;color:#1a3a4a}
.breadcrumb{font-size:11px;color:#8faab5}
.breadcrumb span{margin:0 6px}
.header-right{display:flex;align-items:center;gap:16px}
.date-box{display:flex;align-items:center;gap:8px;padding:6px 12px;border-radius:10px;background:#f5f9fa;color:#5a7a85;font-size:12px}
.date-box span{display:block;font-size:10px}
.date-box strong{font-size:13px;color:#2a4a5a}
.header-divider{width:1px;height:24px;background:#dfe8ec}
.doctor-mini{display:flex;align-items:center;gap:10px}
.mini-avatar{width:32px;height:32px;display:flex;align-items:center;justify-content:center;border-radius:50%;background:linear-gradient(135deg,#087f84,#16a393);color:#fff;font-size:13px;font-weight:700}
.doctor-mini strong{display:block;font-size:13px;color:#1a3a4a}
.doctor-mini span{display:block;font-size:10px;color:#7a9aa5}
.more-button{width:28px;height:28px;border:0;border-radius:8px;background:#f0f5f7;color:#5a7a85;cursor:pointer;transition:.2s}
.more-button:hover{background:#e0e9ec}

/* 通知样式 */
.notification-bell{cursor:pointer;padding:6px;border-radius:8px;transition:.2s;display:flex;align-items:center;color:#5a7a85}
.notification-bell:hover{background:#f0f5f7;color:#087f84}
.notification-badge :deep(.el-badge__content){border:0}
.notification-list{padding:0 8px}
.notification-item{padding:14px 12px;border-radius:10px;margin-bottom:10px;border:1px solid #e8eef0;transition:.2s;cursor:pointer}
.notification-item:hover{background:#f5f9fa}
.notification-item.unread{background:#fff8f0;border-color:#ffd4a3}
.notification-item-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:6px}
.notification-time{font-size:11px;color:#9ab}
.notification-title{font-weight:600;font-size:13px;color:#2a4a5a;margin-bottom:4px}
.notification-content{font-size:12px;color:#5a7a85;line-height:1.5}
.notification-suggestions{margin-top:6px;padding:6px 10px;border-radius:6px;background:#e8f4ff;font-size:11px;color:#2a6a9a}
.notification-empty{padding:40px 0}
</style>
