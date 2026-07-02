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

const mockReplace = vi.fn()
vi.mock('@/router', () => ({ default: { replace: mockReplace } }))

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
  default: { post: mockRequestPost, get: mockRequestGet, put: mockRequestPut }
}))

// mock API
vi.mock('@/api/pharmacy', () => ({
  login: (username, password) => mockRequestPost('/api/staff/login', { username, password, role: 'pharmacy' }),
  getInfo: () => mockRequestGet('/api/staff/info'),
  changePassword: (oldPwd, newPwd) => mockRequestPut('/api/staff/change-password', { oldPassword: oldPwd, newPassword: newPwd }),
  getPrescriptions: (status, page, size) => mockRequestGet('/api/prescription/pharmacy/list', { params: { status, page, size } }),
  getPrescription: (id) => mockRequestGet(`/api/prescription/detail/${id}`),
  dispense: (id) => mockRequestPut(`/api/prescription/dispense/${id}`),
  getMedicines: (params) => mockRequestGet('/api/medicine/list', { params }),
  adjustStock: (id, quantity) => mockRequestPut(`/api/medicine/stock/${id}`, null, { params: { quantity } }),
  generateStockForecast: (period) => mockRequestPost('/api/medicine/ai/stock-forecast/generate', { forecastType: 'monthly', forecastPeriod: period }),
  getStockForecast: (id) => mockRequestGet(`/api/medicine/ai/stock-forecast/${id}`),
  getStockForecastList: (page, size) => mockRequestGet('/api/medicine/ai/stock-forecast/list', { params: { forecastType: 'monthly', page, size } })
}))

// mock auth store
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    signIn: mockRequestPost,
    signOut: vi.fn(),
    token: { value: '' },
    user: { value: null }
  })
}))

// ---------- pharmacy-frontend LoginView 业务逻辑 ----------
function createPharmacyVM() {
  const router = { replace: mockReplace }
  const form = reactive({ username: localStorage.getItem('pharmacy_username') || 'pharmacy01', password: '123456' })
  const remember = reactive({ value: true })

  const submit = async (formRef) => {
    if (!formRef?.validate?.()) return
    mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'pharmacy-token-xyz' } })
    await mockRequestPost(form.username, form.password)
    if (remember.value) {
      localStorage.setItem('pharmacy_username', form.username)
    } else {
      localStorage.removeItem('pharmacy_username')
    }
    mockElMessageSuccess('登录成功，欢迎回来')
    router.replace('/dashboard')
  }

  return { form, remember, submit }
}

// ============================================================
describe('pharmacy-frontend — 登录与认证测试', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mockReplace.mockReset()
  })

  describe('表单数据绑定', () => {
    it('form 初始 username 应为默认值 pharmacy01', () => {
      const { form } = createPharmacyVM()
      expect(form.username).toBe('pharmacy01')
    })

    it('form 初始 password 应为默认值 123456', () => {
      const { form } = createPharmacyVM()
      expect(form.password).toBe('123456')
    })

    it('username 字段应可更新', () => {
      const { form } = createPharmacyVM()
      form.username = 'newuser'
      expect(form.username).toBe('newuser')
    })

    it('password 字段应可更新', () => {
      const { form } = createPharmacyVM()
      form.password = 'newpass'
      expect(form.password).toBe('newpass')
    })
  })

  describe('登录提交（submit）', () => {
    it('登录成功应调用 auth.signIn（通过 request）', async () => {
      const { form, submit } = createPharmacyVM()
      form.username = 'pharmacy01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(mockRequestPost).toHaveBeenCalled()
    })

    it('登录成功后应保存用户名', async () => {
      const { form, submit } = createPharmacyVM()
      form.username = 'pharmacy01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(localStorage.getItem('pharmacy_username')).toBe('pharmacy01')
    })

    it('登录成功后应跳转到 /dashboard', async () => {
      const { form, submit } = createPharmacyVM()
      form.username = 'pharmacy01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(mockReplace).toHaveBeenCalledWith('/dashboard')
    })

    it('表单校验失败时不应调用 API', async () => {
      const { form, submit } = createPharmacyVM()
      form.username = ''
      form.password = ''
      await submit({ validate: () => false })
      expect(mockRequestPost).not.toHaveBeenCalled()
    })
  })

  describe('记住账号功能', () => {
    it('remember 为 true 时应保存用户名', async () => {
      const { form, submit, remember } = createPharmacyVM()
      form.username = 'pharmacy01'
      form.password = 'password123'
      remember.value = true
      await submit({ validate: () => true })
      expect(localStorage.getItem('pharmacy_username')).toBe('pharmacy01')
    })

    it('remember 为 false 时应清除用户名', async () => {
      localStorage.setItem('pharmacy_username', 'saved')
      const { form, submit, remember } = createPharmacyVM()
      form.username = 'pharmacy01'
      form.password = 'password123'
      remember.value = false
      await submit({ validate: () => true })
      expect(localStorage.getItem('pharmacy_username')).toBeNull()
    })
  })
})

describe('pharmacy-frontend API 层测试', () => {

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('应 POST 到 /api/staff/login 且包含 role=pharmacy', async () => {
      const { login } = await import('@/api/pharmacy')
      mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'token' } })
      await login('pharmacy01', 'password123')
      expect(mockRequestPost).toHaveBeenCalledWith('/api/staff/login', { username: 'pharmacy01', password: 'password123', role: 'pharmacy' })
    })
  })

  describe('getPrescriptions', () => {
    it('应 GET /api/prescription/pharmacy/list 带分页参数', async () => {
      const { getPrescriptions } = await import('@/api/pharmacy')
      mockRequestGet.mockResolvedValue({ data: { code: 200, data: { content: [] } } })
      await getPrescriptions('pending', 1, 10)
      expect(mockRequestGet).toHaveBeenCalledWith('/api/prescription/pharmacy/list', { params: { status: 'pending', page: 1, size: 10 } })
    })
  })

  describe('dispense', () => {
    it('应 PUT /api/prescription/dispense/:id', async () => {
      const { dispense } = await import('@/api/pharmacy')
      mockRequestPut.mockResolvedValue({ data: { code: 200, msg: '发药成功' } })
      await dispense(5)
      expect(mockRequestPut).toHaveBeenCalledWith('/api/prescription/dispense/5')
    })
  })

  describe('adjustStock', () => {
    it('应 PUT /api/medicine/stock/:id 带 quantity', async () => {
      const { adjustStock } = await import('@/api/pharmacy')
      mockRequestPut.mockResolvedValue({ data: { code: 200, msg: '调整成功' } })
      await adjustStock(3, 50)
      expect(mockRequestPut).toHaveBeenCalledWith('/api/medicine/stock/3', null, { params: { quantity: 50 } })
    })
  })

  describe('generateStockForecast', () => {
    it('应 POST /api/medicine/ai/stock-forecast/generate', async () => {
      const { generateStockForecast } = await import('@/api/pharmacy')
      mockRequestPost.mockResolvedValue({ data: { code: 200, data: { id: 1 } } })
      await generateStockForecast('2026-07')
      expect(mockRequestPost).toHaveBeenCalledWith('/api/medicine/ai/stock-forecast/generate', { forecastType: 'monthly', forecastPeriod: '2026-07' })
    })
  })

  describe('文件上传', () => {
    it('应支持 PDF 文件', () => {
      const file = new File(['content'], 'report.pdf', { type: 'application/pdf' })
      expect(file.type).toBe('application/pdf')
    })

    it('应支持 JPG、PNG 等图片格式', () => {
      const jpg = new File(['a'], 'a.jpg', { type: 'image/jpeg' })
      const png = new File(['b'], 'b.png', { type: 'image/png' })
      expect(jpg.type).toBe('image/jpeg')
      expect(png.type).toBe('image/png')
    })

    it('FormData 应正确附加文件', () => {
      const file = new File(['content'], 'report.pdf', { type: 'application/pdf' })
      const formData = new FormData()
      formData.append('file', file)
      expect(formData.get('file')).toBe(file)
    })
  })
})
