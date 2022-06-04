
export const info = v => {
  console.log(`[${new Date().toISOString()}] [INFO]  ${v}`)
}

export const error = v => {
  console.log(`[${new Date().toISOString()}] [ERROR] ${v}`)
}

export const warn = v => {
  console.log(`[${new Date().toISOString()}] [WARN]  ${v}`)
}