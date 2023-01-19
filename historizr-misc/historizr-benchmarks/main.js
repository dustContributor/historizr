import { Client } from "https://deno.land/x/postgres@v0.17.0/mod.ts"
import * as log from "https://deno.land/std@0.144.0/log/mod.ts"
import { sleep } from "https://deno.land/x/sleep@v1.2.1/mod.ts";

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

const runAndWait = async procArgs => {
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
  ]);
  proc.close();
  const err = new TextDecoder().decode(stderr)
  const out = new TextDecoder().decode(stdout)
  log.error('Status: ' + status)
  if (err) {
    log.error(err)
  }
  if (out) {
    log.error(out)
  }
  return status.success
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
const deviceReset = await fetch(CFG.device.host + CFG.device.resetEndpoint, {
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
const streamerStatus = await runAndWait(['deno', 'run', '--allow-all',
  CFG.streamer.path,
  CFG.streamer.file,
  CFG.streamer.publishLimit])
log.info('Ran sample process!')
if (!streamerStatus) {
  Deno.exit(1)
}
let storedSamples = 0

while (storedSamples < CFG.streamer.publishLimit) {
  if (storedSamples > 0) {
    log.info('Historized ' + storedSamples + ' of ' + CFG.streamer.publishLimit + ' samples')
  } else {
    log.info('Checking sample historization...')
  }
  await sleep(1)
  const res = await onDatabase(async client =>
    await client.queryObject(`select * from historizr.timing where id = 1`)
  )
  if (!res || res.rowCount < 1) {
    // timing row hasn't been generated yet
    continue
  }
  storedSamples = res.rows[0].counter
}
log.info('Finished historization of ' + storedSamples + ' samples!')

log.info('Moving new timing row...')
await onDatabase(async client =>
  await client.queryArray`
  update historizr.timing
  set id = (select max(id) as id from historizr.timing) + 1, 
  description = ${storedSamples + ' samples'}
  where id = 1`
)
log.info('Moved!')

log.info('Benchmark is finished!')
Deno.exit()