export const CFG = Object.freeze({
  hwmonPath: '/sys/class/hwmon',
  memInfoPath: '/proc/meminfo',
  cpuDevicePath: '/sys/devices/system/cpu',
  brokerUrl: 'mqtt://localhost:1883',
  intervalSeconds: 10,
  mqttClientId: 'historizr-capture',
  destTopic: 'input/',
  retainMessage: false,
  cleanSession: true,
  qualityOfService: 1,
  debug: false
});