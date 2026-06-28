import { beforeEach, vi } from 'vitest'
import { config } from '@vue/test-utils'

globalThis.config = config

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn(), info: vi.fn() }
}))
