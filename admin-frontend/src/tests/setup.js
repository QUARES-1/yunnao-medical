import { vi } from 'vitest'

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    }
  }
})

vi.mock('@element-plus/icons-vue', () => ({
  DataAnalysis: { template: '<span class="icon"></span>' },
  CircleCheckFilled: { template: '<span class="icon"></span>' },
  ArrowRight: { template: '<span class="icon"></span>' },
  Lock: { template: '<span class="icon"></span>' },
  User: { template: '<span class="icon"></span>' },
  FirstAidKit: { template: '<span class="icon"></span>' },
  Right: { template: '<span class="icon"></span>' }
}))
