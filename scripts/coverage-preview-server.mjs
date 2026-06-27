import { createServer } from 'node:http'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { extname, join, normalize, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const workspace = resolve(fileURLToPath(new URL('..', import.meta.url)))
const args = process.argv.slice(2)

const sites = [
  ['Admin-frontend', 5001, 'admin-frontend/coverage'],
  ['Lab-frontend', 5002, 'lab-frontend/coverage'],
  ['Pharmacy-frontend', 5003, 'pharmacy-frontend/coverage'],
  ['Doctor-frontend', 5004, 'doctor-frontend/coverage'],
  ['Patient-frontend', 5005, 'patient-frontend/coverage'],
  ['JaCoCo Backend', 5006, 'target/site/jacoco']
]

const contentTypes = {
  '.css': 'text/css; charset=utf-8',
  '.gif': 'image/gif',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml; charset=utf-8'
}

function resolveRequest(root, requestUrl) {
  const url = new URL(requestUrl, 'http://localhost')
  const requestPath = decodeURIComponent(url.pathname)
  const safeRelativePath = normalize(requestPath).replace(/^(\.\.[/\\])+/, '')
  let filePath = resolve(root, `.${sep}${safeRelativePath}`)

  if (filePath !== root && !filePath.startsWith(root + sep)) {
    return null
  }

  if (existsSync(filePath) && statSync(filePath).isDirectory()) {
    filePath = join(filePath, 'index.html')
  }

  return filePath
}

function serveStatic(name, port, relativeRoot, baseRoot = workspace) {
  const root = resolve(baseRoot, relativeRoot)

  const server = createServer((request, response) => {
    const filePath = resolveRequest(root, request.url ?? '/')

    if (!filePath || !existsSync(filePath) || !statSync(filePath).isFile()) {
      response.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' })
      response.end(`${name}: file not found. Run coverage first if this report is missing.`)
      return
    }

    response.writeHead(200, {
      'cache-control': 'no-store',
      'content-type': contentTypes[extname(filePath).toLowerCase()] ?? 'application/octet-stream'
    })
    createReadStream(filePath).pipe(response)
  })

  server.on('error', error => {
    console.error(`${name}: failed to start on http://localhost:${port}`)
    console.error(error.message)
  })

  server.listen(port, () => {
    console.log(`${name.padEnd(18)} http://localhost:${port}`)
  })
}

if (args.length >= 2) {
  const [relativeRoot, port, name = 'Coverage report'] = args
  console.log('Coverage preview server')
  console.log('-----------------------')
  serveStatic(name, Number(port), relativeRoot, process.cwd())
} else {
  console.log('Coverage preview servers')
  console.log('------------------------')
  for (const site of sites) {
    serveStatic(...site)
  }
}
console.log('')
console.log('Press Ctrl+C to stop all servers.')
