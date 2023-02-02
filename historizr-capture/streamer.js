import { CsvStream } from 'https://deno.land/std@0.165.0/encoding/csv/stream.ts';
import * as log from './log.js'
import { CFG } from './config.js'
import { Broker } from './broker.js'
import { matchOn } from './utils.js'

log.info('opening file...')
const file = await Deno.open(Deno.args[0] || CFG.streamer.file);
const pubLimit = matchOn(Number.parseInt(Deno.args[1]),
  Number.isNaN, 0,
  v => v) || CFG.streamer.publishLimit || 0
const brokerUrl = matchOn(Deno.args[2],
  v => typeof v !== 'string', null,
  v => v.startsWith('mqtt://') ? v : ('mqtt://' + v)) || CFG.brokerUrl || ''
const readable = file.readable
  .pipeThrough(new TextDecoderStream())
  .pipeThrough(new CsvStream());
log.info('opened!')


const tryParseValue = v => {
  switch (typeof v) {
    case 'string': {
      v = v.trim().toLowerCase()
      switch (v) {
        case 'nan': return Number.NaN
        case 'inf':
        case 'infinity': return Number.POSITIVE_INFINITY
        case '-inf':
        case '-infinity': return Number.NEGATIVE_INFINITY
      }
      const parsed = Number.parseFloat(v)
      return Number.isNaN(parsed) ? null : parsed
    }
    case 'number':
    case 'bigint':
      return v
  }
  return null
}

const tryParseId = v => {
  const id = tryParseValue(v)
  return Number.isFinite(id) && id > 0 ? id : null
}

const tryParseQuality = v => {
  switch (typeof v) {
    case 'boolean':
      return v
    case 'string': {
      switch (v.trim().toLowerCase()) {
        case 'true':
        case 't':
          // t, true is true
          return true
        case 'false':
        case 'f':
          // f, false is false
          return false
      }
      return null
    }
    case 'number':
    case 'bigint':
      // 0 is false. anything else is true.
      return v != 0
  }
  return null
}

const tryParseTstamp = v => {
  try {
    return new Date(v).toISOString()
  } catch {
    return null
  }
}

const tryParseName = v => {
  if (typeof v === 'string') {
    v = v.trim()
    return v === '' ? null : v
  }
  return null
}

log.info('connecting to broker...')
const client = await Broker.make({
  brokerUrl: brokerUrl,
  mqttClientId: CFG.mqttClientId,
  cleanSession: CFG.cleanSession
});
log.info('connected!')

const toTopic = v =>
  v.replaceAll('-', '')
    .replaceAll(':', '')
    .replaceAll(' ', '_')
    .replaceAll('__', '_')
    .toLowerCase();

const topicsBySignal = {};

let count = 0
let columnIndices = null

const findColumnIndices = row => row.map(name => ({
  name: name, index: row.findIndex(v => v === CFG.streamer.columns[name])
}))

const findValues = (row, coli) => coli.reduce((p, c) => {
  p[c.name] = row[c.index]
  return p
}, {})

const printStats = (start, count) => {
  const totalTime = (new Date() - start) / 1000.0;
  log.info(`published ${count} messages`)
  log.info(`took ${totalTime.toFixed(3)}s`)
  log.info(`${(totalTime / count).toFixed(3)}s/msg`)
  log.info(`${(count / totalTime).toFixed(3)}msg/s`)
}

log.info('publishing...')
const startTstamp = new Date();
for await (const row of readable) {
  if (columnIndices === null) {
    columnIndices = findColumnIndices(row)
    continue
  }
  const {
    id, tstamp, value, quality, prefix, name
  } = findValues(row, columnIndices)

  const parsedValue = tryParseValue(value)
  if (parsedValue === null) {
    continue
  }
  const parsedTstamp = tryParseTstamp(tstamp)
  if (parsedTstamp === null) {
    continue
  }
  const parsedQuality = tryParseQuality(quality)
  if (parsedQuality === null) {
    // Quality is optional
    parsedQuality = true
  }
  let parsedSignal = null
  const parsedId = tryParseId(id)
  if (parsedId === null) {
    const parsedPrefix = tryParseName(prefix)
    const parsedName = tryParseName(name)
    if (parsedPrefix === null && parsedName === null) {
      // Missing complete signal name
      continue
    }
    if (parsedPrefix === null) {
      parsedSignal = parsedName
    } else if (parsedName === null) {
      parsedSignal = parsedPrefix
    } else {
      parsedSignal = parsedPrefix + '_' + parsedName
    }
  }
  const payload = parsedSignal === null ? {
    id: parsedId,
    tstamp: parsedTstamp,
    value: parsedValue,
    quality: parsedQuality
  } : {
    v: parsedValue,
    t: parsedTstamp,
    q: parsedQuality
  }
  let topic = null
  if (parsedSignal === null) {
    topic = parsedId.toString()
  } else {
    topic = topicsBySignal[parsedSignal]
    if (!topic) {
      topic = toTopic(parsedSignal)
      topicsBySignal[parsedSignal] = topic
    }
  }
  await client.publish(topic, JSON.stringify(payload))
  ++count
  if (pubLimit > 0 && count >= pubLimit) {
    // Reached max publications
    break
  }
  if (count % 100000 == 0) {
    printStats(startTstamp, count)
  }
}
printStats(startTstamp, count)
log.info('finished!')
Deno.exit();
