import { Client } from 'https://deno.land/x/mqtt@0.1.2/deno/mod.ts';
import * as utils from './utils.js'
import * as log from './log.js'
import { CFG } from './config.js'
import { Broker } from './broker.js'

const readAt = v => Deno.readTextFileSync(v).trim();
const readDirAt = v => Deno.readDirSync(utils.dirPathOf(v));

log.info('Initializing capturing methods...')

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

// Now load and pre-parse all the sanitized names for the meminfo
readMemInfo()
  .reduce((prev, curr) => {
    const [key] = curr
    prev[key] = `meminfo_${utils.sanitizeMemName(key)}`
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
log.info('Initialized!')

log.info('Connecting to broker...')
const client = await Broker.make(CFG);
log.info('Connected!')

const publishSimple = async arr => {
  for (const entry of arr) {
    const content = readAt(entry.fullPath)
    await client.publish(entry.name, content)
  }
}
const publishMemInfo = async () => {
  for (const memInfo of readMemInfo()) {
    const [key, value] = memInfo
    const name = memInfoByKey[key]
    if (!name) {
      continue
    }
    await client.publish(name, value)
  }
}

setInterval(async () => {
  await publishSimple(hwmonEntries)
  await publishSimple(cpus)
  await publishMemInfo()
  log.info('Published!')
}, CFG.intervalSeconds * 1000)
log.info('Publishing...')

// const decoder = new TextDecoder();
// let msgCount = 0
// client.on('message', (topic, payload) => {
//   console.log(`message: ${++msgCount}`)
//   console.log(topic, decoder.decode(payload))
// })
// await client.subscribe('/input/#')
