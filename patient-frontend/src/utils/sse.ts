import { API_BASE_URL } from '@/utils/request'

type SseHandlers = {
  onDelta?: (text: string) => void
  onDone?: (data: string) => void
  onError?: (message: string) => void
}

type StreamOptions = {
  url: string
  method?: 'POST' | 'GET'
  data?: Record<string, unknown>
  auth?: boolean
  handlers?: SseHandlers
}

function parseSseChunk(bufferRef: { value: string }, chunk: string, handlers?: SseHandlers) {
  bufferRef.value += chunk
  const parts = bufferRef.value.split(/\r?\n\r?\n/)
  bufferRef.value = parts.pop() || ''
  for (const part of parts) {
    let event = 'message'
    const data: string[] = []
    part.split(/\r?\n/).forEach(line => {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
    })
    const payload = data.join('\n')
    if (event === 'delta') handlers?.onDelta?.(payload)
    else if (event === 'done') handlers?.onDone?.(payload)
    else if (event === 'error') handlers?.onError?.(payload)
  }
}

function decodeBuffer(data: ArrayBuffer) {
  try {
    return new TextDecoder('utf-8').decode(data)
  } catch {
    const bytes = new Uint8Array(data)
    let binary = ''
    bytes.forEach(b => { binary += String.fromCharCode(b) })
    return decodeURIComponent(escape(binary))
  }
}

export function streamSse(options: StreamOptions): Promise<void> {
  const token = uni.getStorageSync('patient_token') as string
  const headers: Record<string, string> = { Accept: 'text/event-stream', 'Content-Type': 'application/json' }
  if (token && options.auth !== false) headers.Authorization = `Bearer ${token}`

  // #ifdef H5
  return new Promise(async (resolve, reject) => {
    try {
      const response = await fetch(`${API_BASE_URL}${options.url}`, {
        method: options.method || 'POST',
        headers,
        body: options.data === undefined ? undefined : JSON.stringify(options.data)
      })
      if (!response.ok || !response.body) throw new Error(`SSE连接失败：${response.status}`)
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      const bufferRef = { value: '' }
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        parseSseChunk(bufferRef, decoder.decode(value, { stream: true }), options.handlers)
      }
      parseSseChunk(bufferRef, decoder.decode(), options.handlers)
      resolve()
    } catch (error) {
      reject(error)
    }
  })
  // #endif

  // #ifndef H5
  return new Promise((resolve, reject) => {
    const bufferRef = { value: '' }
    const task = (uni as any).request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || 'POST',
      data: options.data,
      header: headers,
      enableChunked: true,
      success: () => resolve(),
      fail: () => reject(new Error('SSE连接失败，请检查后端服务'))
    })
    if (task && typeof task.onChunkReceived === 'function') {
      task.onChunkReceived((res: { data: ArrayBuffer }) => {
        parseSseChunk(bufferRef, decodeBuffer(res.data), options.handlers)
      })
    } else {
      reject(new Error('当前运行环境不支持分片流式接收，请使用微信开发者工具或真机调试'))
    }
  })
  // #endif
}
