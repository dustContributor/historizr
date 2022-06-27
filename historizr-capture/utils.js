const NAME_SEPARATOR = '_'
const PATH_SEPARATOR = '/'
export const toCamelCalse = v => {
  if (v.indexOf(NAME_SEPARATOR) < 0) {
    // No separators present
    return v
  }
  const parts = v.split(NAME_SEPARATOR)
  let finalName = null
  for (let i = 0; i < parts.length; ++i) {
    const part = parts[i]
    if (!part) {
      // Empty.
      continue
    }
    if (!finalName) {
      finalName = part
      // First non-empty part
      continue
    }
    // First uppercase, rest as-is
    const first = part[0].toUpperCase()
    finalName += first + part.slice(1)
  }
  return finalName
}

export const tryParse = v => {
  try {
    return JSON.parse(v)
  } catch {
    return null
  }
}

export const ignoreDigits = (v, d) => {
  // Ugly
  return Number.parseFloat(v.toFixed(d || 2))
}

export const trimSeparator = p => trimChar(p, PATH_SEPARATOR)
export const separatorEnd = p => trimSeparator(p) + PATH_SEPARATOR

export const trimChar = (str, ch) => {
  let start = 0
  while (start < str.length && str[start] === ch) {
    ++start
  }
  let end = str.length
  if (start < str.length) {
    while (end > start && str[end - 1] === ch) {
      --end
    }
  }
  if (start > 0 || end < str.length) {
    return str.substring(start, end)
  }
  return str;
}
/**
 * Tests if the passed object is empty, ie, has no properties.
 * @param {*} v any object.
 * @returns true if the object is empty, false otherwise.
 */
export const isEmpty = v => {
  for (const _ in v) {
    return false
  }
  return true
}

export const dirPathOf = v => {
  return `/${trimSeparator(v)}/`;
}

export const sanitizeMemName = v => {
  const str = v.slice(0, -1)
    // Turn any series of upper case letters into PascalCase
    .replaceAll(/[A-Z]{2,}/g, s => `${s[0]}${s.substring(1).toLowerCase()}`)
    .replaceAll('_', '')
    .replaceAll(')', '')
    .replaceAll('(', '_')
    // This will capture 2M 4M etc
    .replaceAll(/\d[A-Z]/g, s => s.toLowerCase())
    .replaceAll(/[A-Z]/g, '_$&')
    .toLowerCase()
  return trimChar(str, '_')
}