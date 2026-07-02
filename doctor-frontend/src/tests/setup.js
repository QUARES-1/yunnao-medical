import { vi } from 'vitest'
import { VueElement } from 'vue'

// ---------- element-plus 全局 mock ----------
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

// ---------- element-plus icons 全局 stub ----------
const createIconStub = () => ({
  template: '<i class="el-icon-stub"></i>'
})

vi.mock('@element-plus/icons-vue', () => ({
  FirstAidKit: createIconStub(),
  CircleCheckFilled: createIconStub(),
  ArrowRight: createIconStub(),
  InfoFilled: createIconStub(),
  Lock: createIconStub(),
  Avatar: createIconStub(),
  User: createIconStub()
}))
