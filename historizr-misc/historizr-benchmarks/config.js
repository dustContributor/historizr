export const CFG = Object.freeze({
  db: Object.freeze({
    host: '',
    database: '',
    user: '',
    pass: '',
    port: 5432
  }),
  debug: false,
  streamer: Object.freeze({
    path: 'streamer.js',
    file: 'data.csv',
    publishLimit: 0
  })
});