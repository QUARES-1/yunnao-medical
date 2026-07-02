import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { streamSse } from '@/utils/sse'

export interface AiMessage {
  role: 'user' | 'ai'
  content: string
  related?: string[]
  department?: string
  departmentId?: number
  streaming?: boolean
}


export const usePatientAiStore = defineStore('patient-ai', () => {
  const auth = useAuthStore()
  const loading = ref(false)
  const streaming = ref(false)
  const sessionId = ref(`patient-${Date.now()}`)
  const messages = ref<AiMessage[]>([
    { role: 'ai', content: '你好呀，我是云脑诊疗 AI 健康顾问。你可以咨询健康问题、就医流程和挂号科室。' }
  ])

  async function ask(question: string) {
    const text = question.trim()
    if (!text || loading.value || streaming.value) return
    messages.value.push({ role: 'user', content: text })
    const index = messages.value.length
    messages.value.push({ role: 'ai', content: '', streaming: true })
    loading.value = false
    streaming.value = true
    try {
      await streamSse({
        url: auth.isLoggedIn() ? '/api/ai/health-consult/stream' : '/api/ai/chat/stream',
        method: 'POST',
        auth: auth.isLoggedIn(),
        data: auth.isLoggedIn()
          ? { question: text, includeHistory: true }
          : { question: text, sessionId: sessionId.value },
        handlers: {
          onDelta: chunk => { messages.value[index].content += chunk },
          onDone: data => {
            try {
              const meta = JSON.parse(data || '{}')
              messages.value[index].related = meta.relatedQuestions || []
              messages.value[index].department = meta.recommendDepartment
              messages.value[index].departmentId = meta.recommendDepartmentId
            } catch {}
          },
          onError: message => { throw new Error(message || 'AI流式回答失败') }
        }
      })
      if (!messages.value[index].content) messages.value[index].content = '暂时没有得到明确答案，建议到线下医院进一步咨询。'
    } finally {
      messages.value[index].streaming = false
      streaming.value = false
      loading.value = false
    }
  }
  function resetConversation() {
    sessionId.value = `patient-${Date.now()}`
    messages.value = [{ role: 'ai', content: '新的咨询已经开始，请描述你现在最关心的问题。' }]
  }

  return { messages, loading, streaming, sessionId, ask, resetConversation }
})

