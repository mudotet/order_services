import test from 'node:test'
import assert from 'node:assert/strict'
import { createDemo } from '../src/demo.js'

const login = handler => handler('/auth/login', { method: 'POST', body: JSON.stringify({ email: 'demo@order.local', password: 'Demo123!' }) })
const send = (handler, path, method, body) => handler(path, { method, body: JSON.stringify(body) }).data

test('demo credentials are exact and session is explicitly separate', () => {
  const entries = new Map()
  const storage = { getItem: key => entries.get(key), setItem: (key, value) => entries.set(key, value), removeItem: key => entries.delete(key) }
  const handler = createDemo(storage)
  assert.equal(handler('/auth/me').handled, false)
  assert.equal(send(handler, '/auth/login', 'POST', { email: 'demo@order.local', password: 'wrong' }), null)
  assert.equal(handler('/catalog').handled, false)
  assert.deepEqual(login(handler).data.roles, ['ADMIN', 'USER', 'SHIPPER'])
  assert.equal(entries.get('order-workspace-demo-session'), 'active')
  const restored = createDemo(storage)
  assert.equal(restored('/auth/me').data.email, 'demo@order.local')
  assert.equal(send(restored, '/auth/logout', 'POST', {}), null)
  assert.equal(createDemo(storage)('/auth/me').handled, false)
  login(handler)
  assert.equal(send(handler, '/auth/login', 'POST', { email: 'real@example.com', password: 'Demo123!' }), null)
  assert.equal(handler('/catalog').handled, false)
})

test('cart mutations and checkout update inventory atomically in isolated memory', () => {
  const handler = createDemo()
  login(handler)
  const initial = handler('/catalog').data.content[0]
  assert.equal(send(handler, '/carts/items', 'POST', { productVariantId: initial.productVariantId, quantity: 1 }).items[0].quantity, 3)
  const cart = handler('/carts').data
  assert.equal(send(handler, `/carts/items/${cart.items[0].cartItemId}/quantity`, 'PATCH', { quantityChange: -1 }).items[0].quantity, 2)
  assert.throws(() => send(handler, '/carts/items', 'POST', { productVariantId: initial.productVariantId, quantity: 999 }), /tồn kho/)
  assert.throws(() => send(handler, `/inventories/${initial.productVariantId}/quantity`, 'PUT', { quantity: -1 }), /Số lượng/)
  const summary = send(handler, '/orders/orders/summary', 'POST', { discountId: 'DEMO10' })
  assert.equal(summary.total, 358000)
  assert.throws(() => send(handler, '/orders', 'POST', { addressId: 'bad', paymentId: 'demo-payment' }), /Không tìm/)
  assert.equal(handler('/carts').data.items[0].quantity, 2)
  const order = send(handler, '/orders', 'POST', { addressId: 'demo-address', paymentId: 'demo-payment', discountId: 'DEMO10' })
  assert.equal(order.total, summary.total)
  assert.equal(handler('/catalog').data.content[0].quantityInStock, initial.quantityInStock - 2)
  assert.equal(handler('/carts').data.items.length, 0)
  assert.equal(handler(`/orders/tracking/${order.id}`).data.state, 'PENDING')
  send(handler, `/inventories/${initial.productVariantId}/quantity`, 'PUT', { quantity: 5 })
  assert.equal(handler('/inventories/products-in-stock').data.content[0].productStockState, 'LIMITED_STOCK')
  const separate = createDemo()
  login(separate)
  assert.equal(separate('/catalog').data.content[0].quantityInStock, 42)
})

test('page, product editing, order and delivery contracts match the workspace', () => {
  const handler = createDemo()
  login(handler)
  assert.equal(handler('/orders?size=2&page=1').data.content.length, 2)
  assert.equal(handler('/orders?state=SHIPPING').data.totalElements, 1)
  assert.equal(handler('/orders/returns?filterBy=REFUNDED').data.content[0].orderReturnStatus, 'REFUNDED')
  assert.equal(handler('/orders/returns/summary!').data.totalRefunds, 185000)
  const product = send(handler, '/products', 'POST', { productId: 'SP-NEW', productName: 'Trà sen', productType: 'Thực phẩm' })
  const variant = send(handler, `/products/${product.productId}/variants`, 'POST', { productVariant: 'Hộp 100g', price: 125000 })
  send(handler, `/inventories/productsInStock/${product.productId}/variants/${variant.productVariantId}`, 'PATCH', { productPrice: 130000, quantityInStock: 12, productDescription: '' })
  assert.equal(handler('/catalog?query=Trà').data.content[0].price, 130000)
  assert.equal(handler('/catalog?query=Trà').data.content[0].description, '')
  const order = handler('/shipper/orders?state=PROCESSING').data.content[0]
  send(handler, `/orders/${order.orderId}/shipper`, 'PATCH', { shipperId: 'demo-user' })
  send(handler, '/shipper/orders/receive', 'POST', { orderId: order.orderId })
  send(handler, '/shipper/orders/delivered', 'POST', { orderId: order.orderId, deliveryAttemptId: order.deliveryAttemptId, customerName: order.customerName, address: order.shippingAddress })
  assert.equal(handler(`/shipper/orders/${order.orderId}`).data.state, 'DELIVERED')
  assert.throws(() => send(handler, `/orders/tracking/${order.orderId}/state`, 'PATCH', { state: 'PENDING' }), /trạng thái/)
  assert.throws(() => handler('/unknown'), /chưa được hỗ trợ/)
})
