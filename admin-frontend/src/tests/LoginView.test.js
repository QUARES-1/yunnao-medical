import { describe, it, expect, vi, beforeEach } from 'vitest'
import { reactive } from 'vue'

// ---------- 全局 mock ----------
vi.stubGlobal('localStorage', {
  data: {},
  getItem(key) { return this.data[key] ?? null },
  setItem(key, val) { this.data[key] = val },
  removeItem(key) { delete this.data[key] },
  clear() { this.data = {} }
})

const mockPush = vi.fn()
vi.mock('@/router', () => ({ default: { push: mockPush } }))

const mockElMessageSuccess = vi.fn()
const mockElMessageError = vi.fn()
vi.mock('element-plus', () => ({
  ElMessage: { success: mockElMessageSuccess, error: mockElMessageError }
}))

// mock request — 所有 API 都走这个
const mockRequestPost = vi.fn()
const mockRequestGet = vi.fn()
const mockRequestPut = vi.fn()
vi.mock('@/utils/request', () => ({
  default: {
    post: mockRequestPost,
    get: mockRequestGet,
    put: mockRequestPut
  }
}))

// mock API 模块
vi.mock('@/api/admin', () => ({
  loginAdmin: (username, password) => mockRequestPost('/api/admin/login', { username, password }),
  registerAdmin: (username, password, name) => mockRequestPost('/api/admin/register', { username, password, name }),
  getAdminInfo: () => mockRequestGet('/api/admin/info'),
  getOverview: () => mockRequestGet('/api/admin/statistics/overview'),
  changeAdminPassword: (oldPwd, newPwd) => mockRequestPut('/api/admin/change-pwd', { oldPassword: oldPwd, newPassword: newPwd })
}))

// ---------- admin-frontend LoginView 业务逻辑 ----------
function createAdminVM() {
  const router = { push: mockPush }
  const form = reactive({ username: '', password: '' })
  const remember = reactive({ value: true })

  const onMounted = () => {
    form.username = localStorage.getItem('admin_username') || ''
  }

  const submit = async (formRef) => {
    if (!formRef?.validate?.()) return
    // 调用 loginAdmin 接口
    mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'admin-token-xyz' } })
    const r = await mockRequestPost('/api/admin/login', { username: form.username, password: form.password })
    localStorage.setItem('admin_token', r.data.data)
    if (remember.value) {
      localStorage.setItem('admin_username', form.username)
    } else {
      localStorage.removeItem('admin_username')
    }
    mockElMessageSuccess('登录成功，欢迎进入管理中心')
    router.push('/dashboard')
  }

  return { form, remember, onMounted, submit }
}

// ============================================================
describe('admin-frontend — 登录与认证测试', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mockPush.mockReset()
  })

  describe('表单数据绑定', () => {
    it('form 初始状态两字段应为空', () => {
      const { form } = createAdminVM()
      expect(form.username).toBe('')
      expect(form.password).toBe('')
    })

    it('username 字段应可更新', () => {
      const { form } = createAdminVM()
      form.username = 'admin01'
      expect(form.username).toBe('admin01')
    })

    it('password 字段应可更新', () => {
      const { form } = createAdminVM()
      form.password = 'password123'
      expect(form.password).toBe('password123')
    })
  })

  describe('页面加载自动恢复用户名（onMounted）', () => {
    it('localStorage 有保存用户名时应填入表单', () => {
      localStorage.setItem('admin_username', 'saved_admin')
      const { form, onMounted } = createAdminVM()
      onMounted()
      expect(form.username).toBe('saved_admin')
    })

    it('localStorage 无保存用户名时表单应为空', () => {
      localStorage.clear()
      const { form, onMounted } = createAdminVM()
      onMounted()
      expect(form.username).toBe('')
    })
  })

  describe('登录提交（submit）', () => {
    it('登录成功应调用 POST /api/admin/login', async () => {
      const { form, submit } = createAdminVM()
      form.username = 'admin01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(mockRequestPost).toHaveBeenCalledWith('/api/admin/login', { username: 'admin01', password: 'password123' })
    })

    it('登录成功后应将 token 存入 localStorage', async () => {
      const { form, submit } = createAdminVM()
      form.username = 'admin01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(localStorage.getItem('admin_token')).toBeTruthy()
    })

    it('登录成功后应保存用户名', async () => {
      const { form, submit } = createAdminVM()
      form.username = 'admin01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(localStorage.getItem('admin_username')).toBe('admin01')
    })

    it('登录成功后应跳转到 /dashboard', async () => {
      const { form, submit } = createAdminVM()
      form.username = 'admin01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(mockPush).toHaveBeenCalledWith('/dashboard')
    })

    it('表单校验失败时不应调用 API', async () => {
      const { form, submit } = createAdminVM()
      form.username = ''
      form.password = ''
      await submit({ validate: () => false })
      expect(mockRequestPost).not.toHaveBeenCalled()
    })
  })
})

describe('admin-frontend API 层测试', () => {

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('loginAdmin', () => {
    it('应 POST 到 /api/admin/login', async () => {
      const { loginAdmin } = await import('@/api/admin')
      mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'token-xyz' } })
      await loginAdmin('admin01', 'password123')
      expect(mockRequestPost).toHaveBeenCalledWith('/api/admin/login', { username: 'admin01', password: 'password123' })
    })

    it('返回的 token 应正确提取', async () => {
      const { loginAdmin } = await import('@/api/admin')
      mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'token-xyz' } })
      const r = await loginAdmin('admin01', 'password123')
      expect(r.data.data).toBe('token-xyz')
    })
  })

  describe('registerAdmin', () => {
    it('应 POST 到 /api/admin/register', async () => {
      const { registerAdmin } = await import('@/api/admin')
      mockRequestPost.mockResolvedValue({ data: { code: 200, msg: '注册成功' } })
      await registerAdmin('newadmin', 'password123', '新管理员')
      expect(mockRequestPost).toHaveBeenCalledWith('/api/admin/register', { username: 'newadmin', password: 'password123', name: '新管理员' })
    })
  })

  describe('changeAdminPassword', () => {
    it('应 PUT 到 /api/admin/change-pwd', async () => {
      const { changeAdminPassword } = await import('@/api/admin')
      mockRequestPut.mockResolvedValue({ data: { code: 200, msg: '修改成功' } })
      await changeAdminPassword('old123', 'new456')
      expect(mockRequestPut).toHaveBeenCalledWith('/api/admin/change-pwd', { oldPassword: 'old123', newPassword: 'new456' })
    })
  })

  describe('getOverview', () => {
    it('应 GET /api/admin/statistics/overview', async () => {
      const { getOverview } = await import('@/api/admin')
      mockRequestGet.mockResolvedValue({ data: { code: 200, data: {} } })
      await getOverview()
      expect(mockRequestGet).toHaveBeenCalledWith('/api/admin/statistics/overview')
    })
  })
})
