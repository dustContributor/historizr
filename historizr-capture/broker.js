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

  async subscribe(topic) {
    return await this.#client.subscribe(topic)
  }

  onReceived(e) {
    return this.#client.on('message', e)
  }

  static async make({
    mqttClientId,
    brokerUrl,
    cleanSession,
    retainMessage,
    qualityOfService,
    destTopic
  }) {
    const client = new Client({
      clientId: mqttClientId,
      url: brokerUrl,
      clean: cleanSession
    })
    await client.connect()

    const pubOpts = {
      retain: retainMessage,
      qos: qualityOfService
    };

    return new Broker({
      client: client,
      pubOpts: pubOpts,
      destTopic: utils.separatorEnd(destTopic)
    })
  }
}