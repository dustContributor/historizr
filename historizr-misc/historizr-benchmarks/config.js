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
    resetEndpoint: '/device/discardsamplestate'
  }),
  debug: false,
  streamer: Object.freeze({
    path: 'streamer.js',
    file: 'data.csv',
    publishLimit: 0
  })
});