import { defineStore } from 'pinia'
import { ref } from 'vue'
import { askAiChat, healthConsult } from '@/api/patient'
import { useAuthStore } from '@/stores/auth'

export interface AiMessage {
  role: 'user' | 'ai'
  content: string
  related?: string[]
  department?: string
  departmentId?: number
  streaming?: boolean
}

const sleep = (ms: number) => new Promise<void>(resolve => setTimeout(resolve, ms))

export const usePatientAiStore = defineStore('patient-ai', () => {
  const auth = useAuthStore()
  const loading = ref(false)
  const streaming = ref(false)
  const sessionId = ref(`patient-${Date.now()}`)
  const messages = ref<AiMessage[]>([
    { role: 'ai', content: '你好呀，我是云脑诊疗 AI 健康顾问。你可以咨询健康问题、就医流程和挂号科室。' }
  ])

  async function streamAnswer(index: number, fullText: string) {
    streaming.value = true
    messages.value[index].streaming = true
    messages.value[index].content = ''
    try {
      for (const char of fullText) {
        messages.value[index].content += char
        await sleep(char === '。' || char === '\n' ? 38 : 16)
      }
    } finally {
      messages.value[index].streaming = false
      streaming.value = false
    }
  }

  async function ask(question: string) {
    const text = question.trim()
    if (!text || loading.value || streaming.value) return
    messages.value.push({ role: 'user', content: text })
    loading.value = true
    try {
      const result = auth.isLoggedIn()
        ? await healthConsult({ question: text, includeHistory: true })
        : await askAiChat({ question: text, sessionId: sessionId.value })
      const index = messages.value.length
      messages.value.push({ role: 'ai', content: '', streaming: true })
      loading.value = false
      await streamAnswer(index, result.answer || '暂时没有得到明确答案，建议到线下医院进一步咨询。')
      messages.value[index].related = result.relatedQuestions || []
      messages.value[index].department = result.recommendDepartment
      messages.value[index].departmentId = result.recommendDepartmentId
    } finally {
      loading.value = false
    }
  }

  function resetConversation() {
    sessionId.value = `patient-${Date.now()}`
    messages.value = [{ role: 'ai', content: '新的咨询已经开始，请描述你现在最关心的问题。' }]
  }

  return { messages, loading, streaming, sessionId, ask, resetConversation }
})
