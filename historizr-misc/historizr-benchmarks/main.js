import { Client } from 'https://deno.land/x/postgres@v0.17.0/mod.ts'
import * as log from 'https://deno.land/std@0.144.0/log/mod.ts'
import { sleep } from 'https://deno.land/x/sleep@v1.2.1/mod.ts'

import { CFG } from './config.js'

const onDatabase = async op => {
  const client = new Client({
    applicationName: 'historizr-benchmarks',
    hostname: CFG.db.host,
    database: CFG.db.database,
    user: CFG.db.user,
    password: CFG.db.pass,
    port: CFG.db.port,
    tls: {
      enabled: false
    }
  })
  await client.connect()
  const res = await op(client)
  await client.end()
  return res
}

const logIf = (cond, to) => {
  if (cond) {
    to(cond)
  }
}

const runAsync = async procArgs => {
  log.info(`Running command: ${procArgs.join(' ')}`)
  const proc = Deno.run({
    cmd: procArgs,
    stderr: 'piped',
    stdout: 'piped'
  })
  const [status, stdout, stderr] = await Promise.all([
    proc.status(),
    proc.output(),
    proc.stderrOutput()
  ])
  proc.close()
  const decoder = new TextDecoder()
  return {
    success: status.success,
    err: decoder.decode(stderr),
    out: decoder.decode(stdout)
  }
}

log.info('Truncating sample table...')
await onDatabase(async client =>
  await client.queryObject('truncate historizr.sample')
)
log.info('Truncated!')

log.info('Moving old timing row...')
await onDatabase(async client =>
  await client.queryObject(`
  update historizr.timing
  set id = (select max(id) as id from historizr.timing) + 1
  where id = 1`)
)
log.info('Moved!')

log.info('Resetting the device state...')
const deviceReset = await fetch(
  `http://${CFG.device.host}:${CFG.device.apiPort}${CFG.device.resetEndpoint}`, {
  method: 'POST'
})
if (deviceReset.status != 200) {
  const text = await deviceReset.text()
  log.error('Failed resetting the device state!')
  log.error(text)
  Deno.exit(1)
}
log.info('Device state reset!')

log.info('Running sample process...')
const streamerStatusPromise = runAsync(['deno', 'run', '--allow-all',
  CFG.streamer.path,
  CFG.streamer.file,
  CFG.streamer.publishLimit,
  `${CFG.device.host}:${CFG.device.brokerPort}`])

const pubLimit = CFG.streamer.publishLimit
let storedSamples = 0
let totalSeconds = 0
let msgsps = 0
let waited = 0
const genStats = () =>
  `name: ${CFG.name || 'UNK'}, `
  + `${storedSamples}/${pubLimit} samples, `
  + `${(storedSamples / pubLimit * 100.0).toFixed(3)}%, `
  + `${msgsps.toFixed(3)}msg/s, `
  + `${totalSeconds.toFixed(3)}s `
log.info('Checking sample historization...')
while (storedSamples < pubLimit) {
  log.info(genStats())
  await sleep(1)
  const res = await onDatabase(async client =>
    await client.queryObject(`select * from historizr.timing_stats where id = 1`)
  )
  const [timing] = res.rows
  msgsps = Number.parseFloat(timing?.msgps) || 0
  totalSeconds = Number.parseFloat(timing?.total_seconds) || 0
  const timingCounter = Number.parseInt(timing?.counter) || 0
  if (timingCounter == storedSamples) {
    if (++waited > CFG.storingTimeout) {
      // something happened, sample count didnt change and we took too long
      log.warning('Took too long to historize new samples, skipping...')
      break
    }
  } else {
    // sample count updated, reset waiting period
    waited = 0
  }
  storedSamples = timingCounter
}
log.info('Finished historization of samples!')
log.info(genStats())

log.info('Waiting for sample process...')
const streamerStatus = await streamerStatusPromise
log.info('Ran sample process!')
logIf(streamerStatus.err, log.error)
logIf(streamerStatus.out, log.info)
if (!streamerStatus.success) {
  Deno.exit(1)
}

log.info('Moving new timing row...')
await onDatabase(async client =>
  await client.queryArray`
  update historizr.timing
  set id = (select max(id) as id from historizr.timing) + 1, 
  description = ${genStats()}
  where id = 1`
)
log.info('Moved!')

log.info('Benchmark is finished!')
Deno.exit()