import { Client } from 'https://deno.land/x/mqtt@0.1.2/deno/mod.ts';
import * as utils from './utils.js';

export class Broker {
  #client
  #pubOpts
  #destTopic

  constructor(o) {
    this.#client = o.client
    this.#pubOpts = o.pubOpts
    this.#destTopic = o.destTopic

  }

  async publish(name, content) {
    await this.#client.publish(`${this.#destTopic}${name}`, content, this.#pubOpts)
  }

  static async make(cfg) {
    const client = new Client({
      clientId: cfg.mqttClientId,
      url: cfg.brokerUrl,
      clean: cfg.cleanSession
    })
    await client.connect()

    const pubOpts = {
      retain: cfg.retainMessage,
      qos: cfg.qualityOfService
    };
    const destTopic = utils.separatorEnd(cfg.destTopic)

    return new Broker({
      client: client,
      pubOpts: pubOpts,
      destTopic: destTopic
    })
  }
}