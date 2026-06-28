<template>
  <div class="profile-page" v-loading="loading">
    <section class="profile-hero">
      <div class="hero-copy">
        <div class="eyebrow">DOCTOR PROFILE</div>
        <h1>医生个人中心</h1>
        <p>完善专业资料，让患者更清晰地了解您的专长与诊疗方向。</p>
      </div>
      <div class="hero-pattern pattern-one"></div>
      <div class="hero-pattern pattern-two"></div>
      <div class="hero-badge"><el-icon><CircleCheckFilled /></el-icon> 资料已同步</div>
    </section>

    <div class="profile-layout">
      <aside class="identity-panel">
        <div class="identity-card">
          <div class="avatar-wrap">
            <div class="doctor-avatar" @click="avatarInput?.click()" v-loading="uploadingAvatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="医生头像" />
              <span v-else>{{ doctorInitial }}</span>
              <div class="avatar-edit">更换头像</div>
            </div>
            <input ref="avatarInput" type="file" accept="image/png,image/jpeg,image/webp" hidden @change="handleAvatarUpload" />
            <span class="online-dot"></span>
          </div>
          <h2>{{ form.name || '未设置姓名' }}</h2>
          <p class="doctor-role">{{ form.title || '医生' }} · {{ form.departmentName || '暂未分配科室' }}</p>
          <div class="verified"><el-icon><SuccessFilled /></el-icon> 医生身份已认证</div>

          <div class="identity-divider"></div>
          <div class="identity-row">
            <span><el-icon><User /></el-icon> 登录账号</span>
            <strong>{{ form.username || '--' }}</strong>
          </div>
          <div class="identity-row">
            <span><el-icon><Phone /></el-icon> 联系电话</span>
            <strong>{{ form.phone || '暂未填写' }}</strong>
          </div>
          <div class="identity-row">
            <span><el-icon><OfficeBuilding /></el-icon> 所属科室</span>
            <strong>{{ form.departmentName || '暂未设置' }}</strong>
          </div>
        </div>

        <div class="completion-card">
          <div class="completion-head">
            <div><span>资料完整度</span><small>完善资料有助于患者选择</small></div>
            <strong>{{ completeness }}%</strong>
          </div>
          <el-progress :percentage="completeness" :show-text="false" :stroke-width="8" color="#16a394" />
          <div class="completion-tip" v-if="completeness < 100"><el-icon><InfoFilled /></el-icon> 继续补充擅长领域与个人简介</div>
          <div class="completion-tip complete" v-else><el-icon><CircleCheckFilled /></el-icon> 您的专业资料已完整</div>
        </div>

        <div class="password-card">
          <div class="password-title"><el-icon><Lock /></el-icon><div><strong>账户安全</strong><small>定期更新登录密码</small></div></div>
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="原密码" />
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="新密码（至少6位）" />
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="确认新密码" />
          <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">修改密码</el-button>
        </div>
      </aside>

      <main class="form-panel">
        <div class="form-heading">
          <div>
            <span class="section-kicker">PROFILE SETTINGS</span>
            <h2>编辑个人资料</h2>
            <p>信息将用于医生工作站及患者端医生主页展示。</p>
          </div>
          <el-tag type="success" effect="light" round><el-icon><Lock /></el-icon> 信息安全保护</el-tag>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="profile-form">
          <div class="form-section-title"><span class="section-icon"><el-icon><User /></el-icon></span><div><strong>基础信息</strong><small>您的身份与联系方式</small></div></div>
          <div class="form-grid">
            <el-form-item label="登录账号">
              <el-input v-model="form.username" disabled size="large"><template #prefix><el-icon><Key /></el-icon></template></el-input>
              <div class="field-hint">登录账号由系统分配，不支持自行修改</div>
            </el-form-item>
            <el-form-item label="医生姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入真实姓名" size="large" clearable><template #prefix><el-icon><UserFilled /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" size="large" clearable><template #prefix><el-icon><Iphone /></el-icon></template></el-input>
            </el-form-item>
            <el-form-item label="专业职称" prop="title">
              <el-select v-model="form.title" placeholder="请选择职称" size="large" style="width:100%">
                <el-option v-for="title in titles" :key="title" :label="title" :value="title" />
              </el-select>
            </el-form-item>
            <el-form-item label="所属科室" prop="departmentId" class="span-two">
              <el-select v-model="form.departmentId" placeholder="请选择所属科室" size="large" filterable style="width:100%" @change="onDeptChange">
                <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </el-form-item>
          </div>

          <div class="section-line"></div>
          <div class="form-section-title"><span class="section-icon mint"><el-icon><FirstAidKit /></el-icon></span><div><strong>专业介绍</strong><small>展示您的专业能力与诊疗经验</small></div></div>
          <div class="form-grid professional-grid">
            <el-form-item label="擅长领域" prop="specialty" class="span-two">
              <el-input v-model="form.specialty" placeholder="例如：脑血管疾病、偏头痛、失眠等神经系统疾病" size="large" maxlength="100" show-word-limit clearable />
              <div class="field-hint">建议填写 3—5 个最具代表性的诊疗方向</div>
            </el-form-item>
            <el-form-item label="个人简介" prop="introduction" class="span-two">
              <el-input v-model="form.introduction" type="textarea" :rows="6" resize="none" placeholder="介绍您的从医经历、临床经验、诊疗理念等信息" maxlength="1000" show-word-limit />
            </el-form-item>
          </div>

          <div class="action-bar">
            <div class="save-note"><el-icon><DocumentChecked /></el-icon><span>保存后，最新资料将同步到患者端医生主页</span></div>
            <div class="actions">
              <el-button size="large" @click="loadData" :disabled="saving">恢复原内容</el-button>
              <el-button type="primary" size="large" @click="handleSave" :loading="saving"><el-icon><Check /></el-icon> 保存修改</el-button>
            </div>
          </div>
        </el-form>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDoctorInfo, updateDoctorInfo, changeDoctorPassword, uploadDoctorAvatar } from '../../api/doctor'
import request from '../../utils/request'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)
const changingPassword = ref(false)
const avatarInput = ref()
const departments = ref([])
const titles = ['住院医师', '主治医师', '副主任医师', '主任医师']
const form = reactive({ username: '', name: '', phone: '', title: '', departmentId: null, departmentName: '', specialty: '', introduction: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const doctorInitial = computed(() => (form.name || '医').slice(0, 1))
const avatarUrl = computed(() => form.avatar ? (form.avatar.startsWith('http') ? form.avatar : `http://localhost:8080${form.avatar}`) : '')
const completeness = computed(() => {
  const fields = [form.name, form.phone, form.title, form.departmentId, form.specialty, form.introduction]
  return Math.round(fields.filter(value => String(value ?? '').trim()).length / fields.length * 100)
})
const validatePhone = (_rule, value, callback) => {
  if (!value || /^1\d{10}$/.test(value)) callback()
  else callback(new Error('请输入正确的11位手机号'))
}
const rules = {
  name: [{ required: true, message: '请填写医生姓名', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  title: [{ required: true, message: '请选择专业职称', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择所属科室', trigger: 'change' }]
}

const onDeptChange = id => {
  const dept = departments.value.find(item => item.id === id)
  if (dept) form.departmentName = dept.name
}

const loadData = async () => {
  loading.value = true
  try {
    const [infoRes, deptRes] = await Promise.all([getDoctorInfo(), request.get('/api/department/list')])
    Object.assign(form, infoRes.data)
    departments.value = deptRes.data || []
  } finally { loading.value = false }
}

const handleSave = async () => {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await updateDoctorInfo({ name: form.name, phone: form.phone, title: form.title, departmentId: form.departmentId, departmentName: form.departmentName, specialty: form.specialty, introduction: form.introduction, avatar: form.avatar })
    localStorage.setItem('doctorInfo', JSON.stringify({ ...form }))
    window.dispatchEvent(new Event('doctor-profile-updated'))
    ElMessage.success({ message: '个人信息更新成功', duration: 2200 })
  } finally { saving.value = false }
}

const handleAvatarUpload = async event => {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过2MB')
    return
  }
  uploadingAvatar.value = true
  try {
    const result = await uploadDoctorAvatar(file)
    form.avatar = result.data
    await updateDoctorInfo({ avatar: form.avatar })
    window.dispatchEvent(new Event('doctor-profile-updated'))
    ElMessage.success('头像更新成功')
  } finally {
    uploadingAvatar.value = false
    event.target.value = ''
  }
}

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('请填写原密码，新密码至少6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await changeDoctorPassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    ElMessage.success('密码修改成功，下次登录请使用新密码')
  } finally { changingPassword.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.profile-page{min-height:100%;padding:4px 4px 36px;color:#17384a}.profile-hero{position:relative;overflow:hidden;min-height:184px;padding:36px 40px;border-radius:24px;background:linear-gradient(128deg,#073b5c 0%,#086e78 48%,#17a18e 100%);color:#fff;box-shadow:0 18px 45px rgba(8,77,93,.18)}.hero-copy{position:relative;z-index:2}.eyebrow,.section-kicker{font-size:11px;font-weight:700;letter-spacing:2.4px;opacity:.68}.profile-hero h1{margin:9px 0 10px;font-size:32px;line-height:1.2}.profile-hero p{font-size:14px;color:rgba(255,255,255,.74)}.hero-pattern{position:absolute;border:1px solid rgba(255,255,255,.13);border-radius:50%}.pattern-one{width:300px;height:300px;right:-80px;top:-145px;box-shadow:0 0 0 52px rgba(255,255,255,.035)}.pattern-two{width:130px;height:130px;right:245px;bottom:-90px}.hero-badge{position:absolute;z-index:2;right:34px;bottom:30px;display:flex;align-items:center;gap:7px;padding:10px 14px;border:1px solid rgba(255,255,255,.2);border-radius:14px;background:rgba(255,255,255,.12);backdrop-filter:blur(10px);font-size:12px}.profile-layout{position:relative;z-index:3;display:grid;grid-template-columns:300px minmax(0,1fr);gap:22px;margin:-22px 20px 0}.identity-panel{display:flex;flex-direction:column;gap:18px}.identity-card,.completion-card,.form-panel{border:1px solid rgba(221,233,235,.9);background:#fff;box-shadow:0 12px 35px rgba(24,64,78,.08)}.identity-card{padding:30px 24px 24px;border-radius:22px;text-align:center}.avatar-wrap{position:relative;width:92px;margin:0 auto}.doctor-avatar{width:92px;height:92px;display:flex;align-items:center;justify-content:center;border:6px solid #edf8f6;border-radius:30px;background:linear-gradient(145deg,#d7f0ec,#b5dfd9);color:#087f84;font-size:35px;font-weight:800;box-shadow:0 9px 22px rgba(8,127,132,.14)}.online-dot{position:absolute;right:-2px;bottom:5px;width:18px;height:18px;border:4px solid #fff;border-radius:50%;background:#25bf84}.identity-card h2{margin:17px 0 6px;font-size:21px}.doctor-role{color:#789098;font-size:13px}.verified{display:inline-flex;align-items:center;gap:5px;margin-top:13px;padding:6px 11px;border-radius:20px;background:#e9f8f3;color:#17896f;font-size:12px}.identity-divider{height:1px;margin:24px 0 12px;background:#edf2f3}.identity-row{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 2px;font-size:12px}.identity-row span{display:flex;align-items:center;gap:7px;color:#8b9ca2}.identity-row strong{max-width:145px;overflow:hidden;color:#405b65;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.completion-card{padding:20px;border-radius:18px}.completion-head{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:13px}.completion-head span,.completion-head small{display:block}.completion-head span{font-size:14px;font-weight:700}.completion-head small{margin-top:4px;color:#99a8ad;font-size:11px}.completion-head strong{color:#0b8a7d;font-size:20px}.completion-tip{display:flex;align-items:center;gap:6px;margin-top:12px;color:#899da4;font-size:11px}.completion-tip.complete{color:#198a70}.form-panel{min-width:0;padding:30px 32px;border-radius:22px}.form-heading{display:flex;align-items:flex-start;justify-content:space-between;padding-bottom:25px}.section-kicker{color:#118d82}.form-heading h2{margin:6px 0 6px;font-size:24px}.form-heading p{color:#8a9ca3;font-size:13px}.form-heading .el-tag{display:flex;gap:5px;height:30px}.profile-form{border-top:1px solid #edf2f3;padding-top:25px}.form-section-title{display:flex;align-items:center;gap:12px;margin-bottom:20px}.section-icon{width:40px;height:40px;display:flex;align-items:center;justify-content:center;border-radius:13px;background:#eaf1ff;color:#4d76bd;font-size:18px}.section-icon.mint{background:#e8f8f4;color:#148c7d}.form-section-title strong,.form-section-title small{display:block}.form-section-title strong{font-size:15px}.form-section-title small{margin-top:3px;color:#9aa9ae;font-size:11px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));column-gap:24px;row-gap:2px}.span-two{grid-column:1/-1}.profile-form :deep(.el-form-item){margin-bottom:20px}.profile-form :deep(.el-form-item__label){padding-bottom:8px;color:#536e78;font-size:13px;font-weight:650}.profile-form :deep(.el-input__wrapper),.profile-form :deep(.el-select__wrapper){min-height:44px;border-radius:11px;box-shadow:0 0 0 1px #dfeaec inset}.profile-form :deep(.el-input__wrapper.is-focus),.profile-form :deep(.el-select__wrapper.is-focused){box-shadow:0 0 0 1px #19a092 inset,0 5px 16px rgba(22,163,148,.10)}.profile-form :deep(.el-textarea__inner){border:1px solid #dfeaec;border-radius:12px;box-shadow:none;line-height:1.7}.field-hint{margin-top:6px;color:#a0adb2;font-size:11px}.section-line{height:1px;margin:4px 0 26px;background:#edf2f3}.action-bar{display:flex;align-items:center;justify-content:space-between;gap:20px;margin:8px -32px -30px;padding:20px 32px;border-radius:0 0 22px 22px;background:#f8fbfb;border-top:1px solid #e7eff0}.save-note{display:flex;align-items:center;gap:8px;color:#82969d;font-size:12px}.save-note .el-icon{color:#159284;font-size:17px}.actions{display:flex;gap:10px}.actions .el-button{min-width:112px;border-radius:11px}.actions .el-button--primary{border-color:#087f84;background:linear-gradient(135deg,#087f84,#16a393);box-shadow:0 7px 18px rgba(8,127,132,.18)}
@media(max-width:1100px){.profile-layout{grid-template-columns:1fr;margin:-18px 10px 0}.identity-panel{display:grid;grid-template-columns:1fr 1fr}.identity-card{grid-row:span 2}.hero-badge{display:none}}@media(max-width:760px){.profile-hero{padding:28px 24px}.profile-layout{margin:-14px 0 0}.identity-panel{display:flex}.form-panel{padding:24px 20px}.form-grid{grid-template-columns:1fr}.span-two{grid-column:auto}.action-bar{align-items:stretch;flex-direction:column;margin:8px -20px -24px;padding:18px 20px}.save-note{justify-content:center}.actions .el-button{flex:1}}
.doctor-avatar{position:relative;overflow:hidden;cursor:pointer}.doctor-avatar img{width:100%;height:100%;object-fit:cover}.avatar-edit{position:absolute;left:0;right:0;bottom:0;padding:6px 0;background:rgba(5,61,74,.72);color:#fff;font-size:10px;opacity:0;transition:.2s}.doctor-avatar:hover .avatar-edit{opacity:1}.password-card{padding:20px;border:1px solid rgba(221,233,235,.9);border-radius:18px;background:#fff;box-shadow:0 12px 35px rgba(24,64,78,.08)}.password-title{display:flex;align-items:center;gap:10px;margin-bottom:14px;color:#1b756f}.password-title>.el-icon{font-size:20px}.password-title strong,.password-title small{display:block}.password-title small{margin-top:3px;color:#98a7ac;font-size:10px}.password-card .el-input{margin-bottom:10px}.password-card :deep(.el-input__wrapper){border-radius:10px}.password-card .el-button{width:100%;border:0;border-radius:10px;background:linear-gradient(135deg,#087f84,#16a393)}
</style>
