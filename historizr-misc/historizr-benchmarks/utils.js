const PATH_SEPARATOR = '/'

export const tryParse = v => {
  try {
    return JSON.parse(v)
  } catch {
    return null
  }
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