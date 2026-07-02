export type SseHandlers = {
  onDelta?: (text: string) => void
  onDone?: (data: string) => void
  onError?: (message: string) => void
}

export async function streamSse(url: string, options: { body?: unknown; token?: string; handlers?: SseHandlers } = {}) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      ...(options.token ? { token: options.token, Authorization: `Bearer ${options.token}` } : {})
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  })
  if (!response.ok || !response.body) throw new Error(`SSE连接失败：${response.status}`)

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const flush = (chunk: string) => {
    buffer += chunk
    const parts = buffer.split(/\r?\n\r?\n/)
    buffer = parts.pop() || ''
    for (const part of parts) {
      let event = 'message'
      const data: string[] = []
      part.split(/\r?\n/).forEach(line => {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
      })
      const payload = data.join('\n')
      if (event === 'delta') options.handlers?.onDelta?.(payload)
      else if (event === 'done') options.handlers?.onDone?.(payload)
      else if (event === 'error') options.handlers?.onError?.(payload)
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    flush(decoder.decode(value, { stream: true }))
  }
  flush(decoder.decode())
}
