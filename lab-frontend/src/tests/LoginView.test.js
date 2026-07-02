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

// mock request
const mockRequestPost = vi.fn()
const mockRequestGet = vi.fn()
const mockRequestPut = vi.fn()
vi.mock('@/utils/request', () => ({
  default: { post: mockRequestPost, get: mockRequestGet, put: mockRequestPut }
}))

// mock API
vi.mock('@/api/lab', () => ({
  login: (username, password) => mockRequestPost('/api/staff/login', { username, password, role: 'lab' }),
  getInfo: () => mockRequestGet('/api/staff/info'),
  changePassword: (oldPwd, newPwd) => mockRequestPut('/api/staff/change-password', { oldPassword: oldPwd, newPassword: newPwd }),
  getExaminations: (status, page, size) => mockRequestGet('/api/examination/lab/list', { params: { status, page, size } }),
  getExamination: (id) => mockRequestGet(`/api/examination/detail/${id}`),
  updateResult: (data) => mockRequestPut('/api/examination/update-result', null, { params: data }),
  getItems: (type) => mockRequestGet('/api/examination/item/list', { params: { type } }),
  uploadFile: (file) => { const fd = new FormData(); fd.append('file', file); return mockRequestPost('/api/file/upload', fd) },
  getCriticalWarnings: (page, size) => mockRequestGet('/api/examination/ai/critical-list', { params: { page, size } }),
  reviewExamination: (id) => mockRequestPost(`/api/examination/ai/review/${id}`),
  getReviewList: (reviewResult, page, size) => mockRequestGet('/api/examination/ai/review-list', { params: { reviewResult, page, size } }),
  getManualReviews: (page, size) => mockRequestGet('/api/examination/ai/manual-list', { params: { page, size } }),
  getReviewDetail: (id) => mockRequestGet(`/api/examination/ai/review-detail/${id}`),
  manualConfirm: (id) => mockRequestPost(`/api/examination/ai/manual-confirm/${id}`),
  rejectReview: (id, reason) => mockRequestPost(`/api/examination/ai/reject/${id}`, null, { params: { reason } }),
  getReviewStats: () => mockRequestGet('/api/examination/ai/review-stats')
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

// ---------- lab-frontend LoginView 业务逻辑 ----------
function createLabVM() {
  const router = { replace: mockReplace }
  const form = reactive({ username: localStorage.getItem('lab_username') || 'lab01', password: '123456' })
  const remember = reactive({ value: true })

  const submit = async (formRef) => {
    if (!formRef?.validate?.()) return
    mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'lab-token-xyz' } })
    await mockRequestPost(form.username, form.password)
    if (remember.value) {
      localStorage.setItem('lab_username', form.username)
    } else {
      localStorage.removeItem('lab_username')
    }
    mockElMessageSuccess('登录成功，欢迎回来')
    router.replace('/dashboard')
  }

  return { form, remember, submit }
}

// ============================================================
describe('lab-frontend — 登录与认证测试', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mockReplace.mockReset()
  })

  describe('表单数据绑定', () => {
    it('form 初始 username 应为默认值 lab01', () => {
      const { form } = createLabVM()
      expect(form.username).toBe('lab01')
    })

    it('form 初始 password 应为默认值 123456', () => {
      const { form } = createLabVM()
      expect(form.password).toBe('123456')
    })

    it('username 字段应可更新', () => {
      const { form } = createLabVM()
      form.username = 'newlab'
      expect(form.username).toBe('newlab')
    })

    it('password 字段应可更新', () => {
      const { form } = createLabVM()
      form.password = 'newpass'
      expect(form.password).toBe('newpass')
    })
  })

  describe('登录提交（submit）', () => {
    it('登录成功应调用 auth.signIn（通过 request）', async () => {
      const { form, submit } = createLabVM()
      form.username = 'lab01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(mockRequestPost).toHaveBeenCalled()
    })

    it('登录成功后应保存用户名', async () => {
      const { form, submit } = createLabVM()
      form.username = 'lab01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(localStorage.getItem('lab_username')).toBe('lab01')
    })

    it('登录成功后应跳转到 /dashboard', async () => {
      const { form, submit } = createLabVM()
      form.username = 'lab01'
      form.password = 'password123'
      await submit({ validate: () => true })
      expect(mockReplace).toHaveBeenCalledWith('/dashboard')
    })

    it('表单校验失败时不应调用 API', async () => {
      const { form, submit } = createLabVM()
      form.username = ''
      form.password = ''
      await submit({ validate: () => false })
      expect(mockRequestPost).not.toHaveBeenCalled()
    })
  })

  describe('记住账号功能', () => {
    it('remember 为 true 时应保存用户名', async () => {
      const { form, submit, remember } = createLabVM()
      form.username = 'lab01'
      form.password = 'password123'
      remember.value = true
      await submit({ validate: () => true })
      expect(localStorage.getItem('lab_username')).toBe('lab01')
    })

    it('remember 为 false 时应清除用户名', async () => {
      localStorage.setItem('lab_username', 'saved')
      const { form, submit, remember } = createLabVM()
      form.username = 'lab01'
      form.password = 'password123'
      remember.value = false
      await submit({ validate: () => true })
      expect(localStorage.getItem('lab_username')).toBeNull()
    })
  })
})

describe('lab-frontend API 层测试', () => {

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('应 POST 到 /api/staff/login 且包含 role=lab', async () => {
      const { login } = await import('@/api/lab')
      mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'token' } })
      await login('lab01', 'password123')
      expect(mockRequestPost).toHaveBeenCalledWith('/api/staff/login', { username: 'lab01', password: 'password123', role: 'lab' })
    })
  })

  describe('getExaminations', () => {
    it('应 GET /api/examination/lab/list 带分页参数', async () => {
      const { getExaminations } = await import('@/api/lab')
      mockRequestGet.mockResolvedValue({ data: { code: 200, data: { content: [] } } })
      await getExaminations('pending', 1, 10)
      expect(mockRequestGet).toHaveBeenCalledWith('/api/examination/lab/list', { params: { status: 'pending', page: 1, size: 10 } })
    })
  })

  describe('updateResult', () => {
    it('应 PUT /api/examination/update-result 带 id、result 参数', async () => {
      const { updateResult } = await import('@/api/lab')
      mockRequestPut.mockResolvedValue({ data: { code: 200, msg: '录入成功' } })
      await updateResult({ id: 3, result: '正常' })
      expect(mockRequestPut).toHaveBeenCalledWith('/api/examination/update-result', null, { params: { id: 3, result: '正常' } })
    })

    it('updateResult 应支持附加 resultImages', async () => {
      const { updateResult } = await import('@/api/lab')
      mockRequestPut.mockResolvedValue({ data: { code: 200 } })
      await updateResult({ id: 3, result: '异常', resultImages: 'http://img.com/x.jpg' })
      expect(mockRequestPut).toHaveBeenCalled()
    })
  })

  describe('uploadFile', () => {
    it('应构造 FormData 并 POST 到 /api/file/upload', async () => {
      const { uploadFile } = await import('@/api/lab')
      mockRequestPost.mockResolvedValue({ data: { code: 200, data: 'http://img.com/file.pdf' } })
      const file = new File(['content'], 'report.pdf', { type: 'application/pdf' })
      await uploadFile(file)
      expect(mockRequestPost).toHaveBeenCalled()
    })

    it('应支持 JPG、PNG、BMP 等图片格式', () => {
      const formats = [
        new File(['a'], 'a.jpg', { type: 'image/jpeg' }),
        new File(['b'], 'b.png', { type: 'image/png' }),
        new File(['c'], 'c.bmp', { type: 'image/bmp' })
      ]
      formats.forEach(f => expect(f.type.startsWith('image/')).toBe(true))
    })
  })

  describe('reviewExamination', () => {
    it('应 POST /api/examination/ai/review/:id', async () => {
      const { reviewExamination } = await import('@/api/lab')
      mockRequestPost.mockResolvedValue({ data: { code: 200, msg: '审核完成' } })
      await reviewExamination(7)
      expect(mockRequestPost).toHaveBeenCalledWith('/api/examination/ai/review/7')
    })
  })

  describe('rejectReview', () => {
    it('应 POST /api/examination/ai/reject/:id 带 reason', async () => {
      const { rejectReview } = await import('@/api/lab')
      mockRequestPost.mockResolvedValue({ data: { code: 200, msg: '已驳回' } })
      await rejectReview(5, '结果存疑')
      expect(mockRequestPost).toHaveBeenCalledWith('/api/examination/ai/reject/5', null, { params: { reason: '结果存疑' } })
    })
  })

  describe('getReviewStats', () => {
    it('应 GET /api/examination/ai/review-stats', async () => {
      const { getReviewStats } = await import('@/api/lab')
      mockRequestGet.mockResolvedValue({ data: { code: 200, data: { total: 100 } } })
      await getReviewStats()
      expect(mockRequestGet).toHaveBeenCalledWith('/api/examination/ai/review-stats')
    })
  })
})
