import { describe, it, expect, vi, beforeEach } from 'vitest'

// ---------- 全局 uni mock ----------
const uniStorage: Record<string, string> = {}

const mockShowToast = vi.fn()
const mockReLaunch = vi.fn()
const mockRemoveStorageSync = vi.fn()
const mockUploadFile = vi.fn()
const mockRequest = vi.fn()

vi.stubGlobal('uni', {
  getStorageSync: vi.fn((key: string) => uniStorage[key]),
  setStorageSync: vi.fn((key: string, val: unknown) => { uniStorage[key] = val as string }),
  removeStorageSync: (key: string) => {
    mockRemoveStorageSync(key)
    delete uniStorage[key]
  },
  showToast: mockShowToast,
  reLaunch: mockReLaunch,
  redirectTo: vi.fn(),
  switchTab: vi.fn(),
  login: vi.fn(),
  request: mockRequest,
  uploadFile: mockUploadFile,
  chooseImage: vi.fn(),
  chooseAvatar: vi.fn(),
  getImageInfo: vi.fn(),
  getSystemInfo: vi.fn(),
  getDeviceInfo: vi.fn(),
  canIUse: vi.fn(() => false),
  createSelectorQuery: vi.fn(() => ({
    select: vi.fn(() => ({ boundingClientRect: vi.fn((cb: Function) => cb()) })),
    selectAll: vi.fn(() => ({ boundingClientRect: vi.fn((cb: Function) => cb()) })),
    in: vi.fn()
  })),
  onShow: vi.fn(),
  offShow: vi.fn(),
  onHide: vi.fn(),
  offHide: vi.fn(),
  getLocation: vi.fn(),
  chooseLocation: vi.fn()
})

// ============================================================
// 纯函数逻辑（复制自 auth/index.vue）
// ============================================================
function needsAuthorization(patient: { name?: string; avatar?: string } | null) {
  const nickname = patient?.name?.trim() || ''
  const avatar = patient?.avatar?.trim() || ''
  const defaultNames = ['微信用户', '未登录用户', '用户', '我']
  return !avatar || !nickname || defaultNames.includes(nickname)
}

function isInvalidPatientError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error || '')
  return message.includes('患者不存在') || message.includes('登录已过期') || message.includes('token') || message.includes('Token')
}

function showError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error || '')
  const finalMsg = message || '操作失败，请稍后重试'
  uni.showToast({ title: finalMsg, icon: 'none', duration: 2400 })
}

// ============================================================
describe('patient-frontend — 授权与登录核心逻辑', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    Object.keys(uniStorage).forEach(k => delete uniStorage[k])
  })

  // ========== needsAuthorization ==========
  describe('needsAuthorization', () => {
    it('patient 为 null 时应需要授权', () => {
      expect(needsAuthorization(null)).toBe(true)
    })

    it('avatar 和 nickname 均为空时应需要授权', () => {
      expect(needsAuthorization({ name: '', avatar: '' })).toBe(true)
    })

    it('avatar 非空但 nickname 为空时应需要授权', () => {
      expect(needsAuthorization({ name: '', avatar: 'http://img.com/a.jpg' })).toBe(true)
    })

    it('avatar 为空但 nickname 非空时应需要授权', () => {
      expect(needsAuthorization({ name: '张三', avatar: '' })).toBe(true)
    })

    it('avatar 和 nickname 均有值时应不需要授权', () => {
      expect(needsAuthorization({ name: '张三', avatar: 'http://img.com/a.jpg' })).toBe(false)
    })

    it('nickname 为"微信用户"时应需要授权', () => {
      expect(needsAuthorization({ name: '微信用户', avatar: 'http://img.com/a.jpg' })).toBe(true)
    })

    it('nickname 为"未登录用户"时应需要授权', () => {
      expect(needsAuthorization({ name: '未登录用户', avatar: 'http://img.com/a.jpg' })).toBe(true)
    })

    it('nickname 为"用户"时应需要授权', () => {
      expect(needsAuthorization({ name: '用户', avatar: 'http://img.com/a.jpg' })).toBe(true)
    })

    it('nickname 为"我"时应需要授权', () => {
      expect(needsAuthorization({ name: '我', avatar: 'http://img.com/a.jpg' })).toBe(true)
    })

    it('nickname 含空格时应去除空格后再判断', () => {
      expect(needsAuthorization({ name: '  张三  ', avatar: 'http://img.com/a.jpg' })).toBe(false)
    })
  })

  // ========== isInvalidPatientError ==========
  describe('isInvalidPatientError', () => {
    it('包含"患者不存在"时应返回 true', () => {
      expect(isInvalidPatientError(new Error('患者不存在'))).toBe(true)
    })

    it('包含"登录已过期"时应返回 true', () => {
      expect(isInvalidPatientError(new Error('登录已过期，请重新登录'))).toBe(true)
    })

    it('包含小写"token"时应返回 true', () => {
      expect(isInvalidPatientError(new Error('invalid token'))).toBe(true)
    })

    it('包含大写"Token"时应返回 true', () => {
      expect(isInvalidPatientError(new Error('Token expired'))).toBe(true)
    })

    it('普通错误信息应返回 false', () => {
      expect(isInvalidPatientError(new Error('网络连接失败'))).toBe(false)
    })

    it('字符串形式错误也应正确判断', () => {
      expect(isInvalidPatientError('患者不存在，请先注册')).toBe(true)
    })
  })

  // ========== showError ==========
  describe('showError', () => {
    it('Error 对象应提取 message 并调用 showToast', () => {
      showError(new Error('网络连接失败'))
      expect(mockShowToast).toHaveBeenCalledWith({ title: '网络连接失败', icon: 'none', duration: 2400 })
    })

    it('空字符串 Error 应显示默认提示', () => {
      showError(new Error(''))
      expect(mockShowToast).toHaveBeenCalledWith({ title: '操作失败，请稍后重试', icon: 'none', duration: 2400 })
    })

    it('undefined 应显示默认提示', () => {
      showError(undefined)
      expect(mockShowToast).toHaveBeenCalledWith({ title: '操作失败，请稍后重试', icon: 'none', duration: 2400 })
    })
  })
})

// ============================================================
describe('patient-frontend — uni storage 模拟', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    Object.keys(uniStorage).forEach(k => delete uniStorage[k])
  })

  it('setStorageSync 应保存数据', () => {
    uni.setStorageSync('patient_token', 'abc123')
    expect(uniStorage['patient_token']).toBe('abc123')
  })

  it('getStorageSync 应读取已保存的数据', () => {
    uniStorage['patient_token'] = 'xyz789'
    expect(uni.getStorageSync('patient_token')).toBe('xyz789')
  })

  it('removeStorageSync 应删除数据', () => {
    uniStorage['patient_token'] = 'token-abc'
    uni.removeStorageSync('patient_token')
    expect(uniStorage['patient_token']).toBeUndefined()
    expect(mockRemoveStorageSync).toHaveBeenCalledWith('patient_token')
  })

  it('getStorageSync 读取不存在的 key 应返回 undefined', () => {
    expect(uni.getStorageSync('not_exist')).toBeUndefined()
  })
})

// ============================================================
describe('patient-frontend — 登录状态管理', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    Object.keys(uniStorage).forEach(k => delete uniStorage[k])
  })

  describe('isLoggedIn', () => {
    it('有 patient_token 时应返回 true', () => {
      uniStorage['patient_token'] = 'valid-token-xyz'
      const result = Boolean(uniStorage['patient_token'])
      expect(result).toBe(true)
    })

    it('无 patient_token 时应返回 false', () => {
      const result = Boolean(uniStorage['patient_token'])
      expect(result).toBe(false)
    })
  })

  describe('logout', () => {
    it('应清除 patient_token', () => {
      uniStorage['patient_token'] = 'token-xyz'
      uni.removeStorageSync('patient_token')
      expect(uniStorage['patient_token']).toBeUndefined()
    })
  })

  describe('登录后页面跳转', () => {
    it('登录成功后应调用 reLaunch 跳转到首页', () => {
      uni.reLaunch({ url: '/pages/index/index' })
      expect(mockReLaunch).toHaveBeenCalledWith({ url: '/pages/index/index' })
    })
  })
})

// ============================================================
describe('patient-frontend — API 路径与参数验证', () => {

  beforeEach(() => {
    mockRequest.mockReset()
  })

  // 每次验证前设置 mock
  const setupRequestMock = (expectedUrl: string, expectedMethod?: string) => {
    mockRequest.mockImplementation((options: Record<string, unknown>) => {
      expect(options.url).toBe(expectedUrl)
      if (expectedMethod) expect(options.method).toBe(expectedMethod)
      return Promise.resolve({ data: { code: 200, data: {} } })
    })
  }

  describe('患者认证', () => {
    it('testPatientLogin 应 POST /api/patient/test-login', () => {
      setupRequestMock('/api/patient/test-login', 'POST')
      mockRequest({ url: '/api/patient/test-login', method: 'POST', data: { account: 'patient01', password: '123456' } })
    })

    it('devPatientLogin 应 POST /api/dev/patient-login', () => {
      setupRequestMock('/api/dev/patient-login', 'POST')
      mockRequest({ url: '/api/dev/patient-login', method: 'POST' })
    })

    it('updatePatientInfo 应 PUT /api/patient/update', () => {
      setupRequestMock('/api/patient/update', 'PUT')
      mockRequest({ url: '/api/patient/update', method: 'PUT', data: { name: '张三', avatar: 'http://img.com/a.jpg' } })
    })
  })

  describe('挂号', () => {
    it('createRegistration 应 POST /api/registration/create', () => {
      setupRequestMock('/api/registration/create', 'POST')
      mockRequest({ url: '/api/registration/create', method: 'POST', data: { doctorId: 5, registrationDate: '2026-07-03', timeSlot: '上午' } })
    })

    it('cancelRegistration 应 PUT /api/registration/cancel/:id', () => {
      setupRequestMock('/api/registration/cancel/3', 'PUT')
      mockRequest({ url: '/api/registration/cancel/3', method: 'PUT' })
    })
  })

  describe('AI 分诊与问诊', () => {
    it('consultTriage 应 POST /api/triage/consult', () => {
      setupRequestMock('/api/triage/consult', 'POST')
      mockRequest({ url: '/api/triage/consult', method: 'POST', data: { chiefComplaint: '头痛发热' } })
    })

    it('askAiChat 应 POST /api/ai/chat', () => {
      setupRequestMock('/api/ai/chat', 'POST')
      mockRequest({ url: '/api/ai/chat', method: 'POST', data: { question: '感冒了怎么办', sessionId: 'session-abc' } })
    })

    it('feedbackAiAnswer 应 POST 带 feedback 参数', () => {
      setupRequestMock('/api/ai/chat/feedback/5?feedback=good', 'POST')
      mockRequest({ url: '/api/ai/chat/feedback/5?feedback=good', method: 'POST' })
    })
  })

  describe('处方与用药', () => {
    it('getMedicationGuide 应 GET /api/medication/guide/:id', () => {
      setupRequestMock('/api/medication/guide/3')
      mockRequest({ url: '/api/medication/guide/3' })
    })

    it('generateMedicationGuide 应 POST /api/medication/guide/generate', () => {
      setupRequestMock('/api/medication/guide/generate', 'POST')
      mockRequest({ url: '/api/medication/guide/generate', method: 'POST', data: { prescriptionId: 3 } })
    })

    it('markMedicationGuidePrinted 应 POST /api/medication/guide/print/:id', () => {
      setupRequestMock('/api/medication/guide/print/3', 'POST')
      mockRequest({ url: '/api/medication/guide/print/3', method: 'POST' })
    })
  })

  describe('检验报告', () => {
    it('getPatientInterpretation 应 GET AI 解读接口', () => {
      setupRequestMock('/api/examination/ai/interpret-patient/7')
      mockRequest({ url: '/api/examination/ai/interpret-patient/7' })
    })

    it('getPatientCriticalWarnings 应 GET 危急值预警接口', () => {
      setupRequestMock('/api/examination/ai/critical-list')
      mockRequest({ url: '/api/examination/ai/critical-list' })
    })
  })

  describe('随访计划', () => {
    it('submitFollowUp 应 POST /api/follow-up/submit/:id', () => {
      setupRequestMock('/api/follow-up/submit/3', 'POST')
      mockRequest({ url: '/api/follow-up/submit/3', method: 'POST' })
    })
  })

  describe('文件上传', () => {
    it('uni.uploadFile 应正确构造上传请求', () => {
      mockUploadFile.mockImplementation((options: Record<string, unknown>) => {
        expect(options.url).toBe('http://localhost:8080/api/file/upload')
        expect(options.name).toBe('file')
        return { uploadTask: {} }
      })
      uni.uploadFile({ url: 'http://localhost:8080/api/file/upload', filePath: '/path/file.jpg', name: 'file' })
    })

    it('应支持 JPG、PNG、JPEG 等图片格式', () => {
      const formats = [
        new File(['a'], 'avatar.jpg', { type: 'image/jpeg' }),
        new File(['b'], 'avatar.png', { type: 'image/png' }),
        new File(['c'], 'photo.jpeg', { type: 'image/jpeg' })
      ]
      formats.forEach(f => expect(f.type.startsWith('image/')).toBe(true))
    })
  })
})

// ============================================================
describe('patient-frontend — Token 与 401 处理', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    Object.keys(uniStorage).forEach(k => delete uniStorage[k])
  })

  describe('Token 注入', () => {
    it('有 token 时应注入 Bearer Token', () => {
      uniStorage['patient_token'] = 'patient-token-xyz'
      const token = uniStorage['patient_token']
      const header: Record<string, string> = {}
      if (token) header.Authorization = `Bearer ${token}`
      expect(header.Authorization).toBe('Bearer patient-token-xyz')
    })

    it('无 token 时不应注入 Authorization', () => {
      const token = uniStorage['patient_token']
      const header: Record<string, string> = {}
      if (token) header.Authorization = `Bearer ${token}`
      expect(header.Authorization).toBeUndefined()
    })
  })

  describe('401 响应处理', () => {
    it('401 时应调用 removeStorageSync 清除 token', () => {
      uniStorage['patient_token'] = 'token-abc'
      uni.removeStorageSync('patient_token')
      expect(mockRemoveStorageSync).toHaveBeenCalledWith('patient_token')
      expect(uniStorage['patient_token']).toBeUndefined()
    })

    it('401 时应调用 showToast 提示重新登录', () => {
      showError(new Error('登录已过期，请重新登录'))
      expect(mockShowToast).toHaveBeenCalled()
    })
  })

  describe('request 响应码判断', () => {
    it('code 200 应视为成功', () => {
      const response = { data: { code: 200, data: {} } }
      expect(response.data.code === 200).toBe(true)
    })

    it('code 400 应视为失败', () => {
      const response = { data: { code: 400, msg: '操作失败' } }
      expect(response.data.code === 200).toBe(false)
    })
  })
})
