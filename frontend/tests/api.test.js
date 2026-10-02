import test from 'node:test'
import assert from 'node:assert/strict'
import { api, send, list, money, transitions } from '../src/api.js'

test('API includes session cookie and decodes data envelope', async () => {
  const original = globalThis.fetch
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/carts/items')
    assert.equal(options.credentials, 'include')
    assert.equal(options.headers['Content-Type'], 'application/json')
    assert.deepEqual(JSON.parse(options.body), { productVariantId: 'v1', quantity: 1 })
    return { ok: true, json: async () => ({ code: 'SUCCESS', data: { quantity: 1 } }) }
  }
  try { assert.deepEqual(await send('/carts/items', 'POST', { productVariantId: 'v1', quantity: 1 }), { quantity: 1 }) } finally { globalThis.fetch = original }
})

test('API rejects security errors and false business result', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async () => ({ ok: false, status: 401, json: async () => { throw new Error('not JSON') } })
    await assert.rejects(api('/auth/me'), error => error.status === 401)
    globalThis.fetch = async () => ({ ok: true, status: 200, json: async () => ({ code: 'SUCCESS', data: false }) })
    await assert.rejects(send('/products', 'POST', {}))
  } finally { globalThis.fetch = original }
})

test('normalizes lists, currency and terminal states', () => {
  assert.deepEqual(list({ content: [1] }), [1])
  assert.deepEqual(list([2]), [2])
  assert.deepEqual(list(null), [])
  assert.match(money(30000), /30\.000/)
  assert.equal(transitions.DELIVERED, undefined)
  assert.equal(transitions.CANCELLED, undefined)
})
