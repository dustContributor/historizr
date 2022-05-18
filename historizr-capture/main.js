import { Client } from 'https://deno.land/x/mqtt@0.1.2/deno/mod.ts';
import * as utils from './utils.js'
import { CFG } from './config.js'

const readAt = v => Deno.readTextFileSync(v).trim();
const readDirAt = v => Deno.readDirSync(utils.dirPathOf(v));

// CPU frequency //

const cpus = []
const baseCpuDevice = utils.dirPathOf(CFG.cpuDevicePath)
for (const cpuFolder of readDirAt(baseCpuDevice)) {
  if (!cpuFolder.isDirectory || !cpuFolder.name.match(/cpu\d/g)) {
    continue
  }
  cpus.push({
    name: cpuFolder.name + '_freq',
    fullPath: `${utils.dirPathOf(baseCpuDevice + cpuFolder.name)}cpufreq/scaling_cur_freq`
  })
}


// Memory info //

const baseMemInfo = CFG.memInfoPath
const memInfoByKey = {}

const readMemInfo = () => {
  // each entry looks like this:
  // VmallocUsed:       12340 kB
  return readAt(baseMemInfo)
    .split('\n')
    .map(e => e.split(' ').filter(e => !!e))
    // Pop the 'kB' at the end. Only keep it if we have key/prefix + value
    .filter(e => e.length == 2 || (e.pop() && e.length == 2))
}

const sanitizeMemName = v => {
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
  return utils.trimChar(str, '_')
}
// Now load and pre-parse all the sanitized names for the meminfo
readMemInfo()
  .reduce((prev, curr) => {
    const [key] = curr
    prev[key] = `meminfo_${sanitizeMemName(key)}`
    return prev
  }, memInfoByKey)


// Hwmon thermals, fans, etc ///

const baseHwmon = utils.dirPathOf(CFG.hwmonPath)
const hwmonEntries = []
// Load all entries for hwmon with their sanitized names
for (const hwmon of readDirAt(baseHwmon)) {
  if (hwmon.isFile) {
    continue
  }
  for (const entry of readDirAt(baseHwmon + hwmon.name)) {
    if (!entry.isFile) {
      continue
    }
    if ('uevent' == entry.name) {
      continue
    }
    try {
      // Just try a read to see if it's valid
      const fullPath = `${utils.dirPathOf(baseHwmon + hwmon.name) + entry.name}`
      const _ = readAt(fullPath)
      hwmonEntries.push({
        name: `${hwmon.name}_${entry.name}`,
        fullPath: fullPath
      });
    } catch (error) {
      if (error.name === 'PermissionDenied') {
        // Nothing to do
        continue
      }
      if (error.message.toLowerCase().startsWith('no data available')) {
        // Nothing to do
        continue
      }
      throw error
    }
  }
}

const client = new Client({
  clientId: CFG.mqttClientId,
  url: CFG.brokerUrl,
  clean: CFG.cleanSession
})
await client.connect()
console.log('Connected!')

const pubOpts = {
  retain: CFG.retainMessage,
  qos: CFG.qualityOfService
};
const destTopic = utils.dirPathOf(CFG.destTopic)

const publishSimple = async arr => {
  for (const entry of arr) {
    const content = readAt(entry.fullPath)
    await client.publish(`${destTopic}${entry.name}`, content, pubOpts)
  }
}
const publishMemInfo = async () => {
  for (const memInfo of readMemInfo()) {
    const [key, value] = memInfo
    const name = memInfoByKey[key]
    if (!name) {
      continue
    }
    await client.publish(`${destTopic}${name}`, value, pubOpts)
  }
}
setInterval(async () => {
  await publishSimple(hwmonEntries)
  await publishSimple(cpus)
  await publishMemInfo()
}, CFG.intervalSeconds * 1000)

console.log('Publishing...')
// const decoder = new TextDecoder();
// let msgCount = 0
// client.on('message', (topic, payload) => {
//   console.log(`message: ${++msgCount}`)
//   console.log(topic, decoder.decode(payload))
// })
// await client.subscribe('/input/#')
