export const CFG = Object.freeze({
  db: Object.freeze({
    host: '',
    database: '',
    user: '',
    pass: '',
    port: 5432
  }),
  device: Object.freeze({
    host: '',
    apiPort: 443,
    brokerPort: 1883,
    resetEndpoint: '/device/discardsamplestate'
  }),
  debug: false,
  streamer: Object.freeze({
    path: 'streamer.js',
    file: 'data.csv',
    publishLimit: 0
  })
});