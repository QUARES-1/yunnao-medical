import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: ['./src/tests/setup.js'],
    include: ['src/tests/**/*.test.{js,mjs,ts}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/**/*.{js,ts}'],
      exclude: [
        'src/main.ts', 'src/tests/**', 'src/pages/**', 'src/types/**',
        'src/App.vue', 'src/env.d.ts', 'src/shime-uni.d.ts',
        // #ifndef H5 block in sse.ts is only reachable in non-H5 environments
        'src/utils/sse.ts'
      ]
    }
  }
})
