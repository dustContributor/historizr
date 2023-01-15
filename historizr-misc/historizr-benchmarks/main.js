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
    port: CFG.db.port
  })
  await client.connect()
  await op(client)
  await client.end()
}

const runAndWait = async procArgs => {
  const proc = Deno.run({
    cmd: procArgs
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
}

log.info('Truncating sample table...')
await onDatabase(async client =>
  await client.queryObject('truncate historizr.sample')
)
log.info('Truncated!')

log.info('Moving old timing row...')
await onDatabase(async client =>
  await client.queryObject(`
  with maxid as (select max(id) + 1 as id from historizr.timing)
  update historizr.timing
  set id = maxid.id
  where id = 1`)
)
log.info('Moved!')

log.info('Running sample process...')
await runAndWait(['deno', 'run', '--allow-all',
  CFG.streamer.path,
  CFG.streamer.file,
  CFG.streamer.publishLimit])
log.info('Ran sample process!')

for (let storedSamples = 0; storedSamples <= CFG.streamer.publishLimit;) {
  if (storedSamples > 0) {
    log.info('Historized ' + storedSamples + ' samples so far...')
  } else {
    log.info('Checking sample historization...')
  }
  await sleep(1000)
  const res = await client.queryObject(`select * from historizr.timing where id = 1`)
  storedSamples = res.counter
}
log.info('Finished historization of ' + storedSamples + ' samples!')

log.info('Moving new timing row...')
await onDatabase(async client =>
  await client.queryObject(`
  with maxid as (select max(id) + 1 as id from historizr.timing)
  update historizr.timing
  set 
    id = maxid.id,
    desc = $1
  where id = 1`, [storedSamples + ' samples'])
)
log.info('Moved!')
Deno.exit()