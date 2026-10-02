const sessionKey = 'order-workspace-demo-session'
const account = { userId: 'demo-user', userName: 'Tài khoản Demo · Nguyễn Minh', email: 'demo@order.local', roles: ['ADMIN', 'USER', 'SHIPPER'] }
const fail = (message, status = 400) => { throw Object.assign(new Error(message), { status }) }
const copy = value => structuredClone(value)
const stockState = quantity => quantity === 0 ? 'OUT_OF_STOCK' : quantity < 10 ? 'LIMITED_STOCK' : 'IN_STOCK'
const transitions = { PENDING: ['CONFIRMED', 'PROCESSING', 'CANCELLED'], CONFIRMED: ['PROCESSING', 'CANCELLED'], PROCESSING: ['SHIPPING', 'CANCELLED'], SHIPPING: ['DELIVERED', 'DELIVERY_FAILED'], DELIVERY_FAILED: ['SHIPPING'] }

export function createDemo(storage = null) {
  let active = false
  try { active = storage?.getItem(sessionKey) === 'active' } catch { active = false }
  let sequence = 10
  const products = [
    { productId: 'SP-001', productName: 'Cà phê Đắk Lắk', productType: 'Thực phẩm' },
    { productId: 'SP-002', productName: 'Bình giữ nhiệt', productType: 'Gia dụng' },
    { productId: 'SP-003', productName: 'Túi vải Hội An', productType: 'Phụ kiện' },
  ]
  const variants = products.map((product, index) => ({ ...product, productVariantId: `BT-00${index + 1}`, productVariant: ['Arabica 500g', 'Inox 500ml · xanh', 'Vải canvas · tự nhiên'][index], price: [185000, 249000, 95000][index], quantityInStock: [42, 8, 0][index] }))
  const addresses = [{ addressId: 'demo-address', address: '12 Nguyễn Huệ, Bến Nghé', city: 'TP. Hồ Chí Minh' }]
  const payments = [{ paymentId: 'demo-payment', paymentMethod: 'Thanh toán khi nhận hàng (Demo)' }]
  const discounts = [{ discountId: 'DEMO10', discountType: 'PERCENTAGE', discountValue: 10 }]
  const shippers = [{ userId: 'demo-user', shipperId: 'demo-user', userName: 'Nguyễn Minh · Shipper Demo', email: account.email }]
  let cart = [{ cartItemId: 'demo-cart-1', productVariantId: 'BT-001', quantity: 2 }]
  const orders = ['PENDING', 'PROCESSING', 'SHIPPING', 'DELIVERED', 'DELIVERY_FAILED'].map((state, index) => ({
    id: `DEMO-${1001 + index}`, orderId: `DEMO-${1001 + index}`, state, total: 395000 + index * 64000,
    createdAt: new Date(Date.now() - index * 86400000).toISOString(), customerName: ['Nguyễn Minh', 'Trần Lan', 'Lê Hoàng', 'Phạm Mai', 'Võ Anh'][index],
    recipientName: 'Nguyễn Minh', shippingAddress: addresses[0].address, shippingCity: addresses[0].city,
    paymentMethodInfo: payments[0].paymentMethod, estimatedDelivery: new Date(Date.now() + 2 * 86400000).toISOString(), daysRemaining: 2,
    shipperId: 'demo-user', deliveryAttemptId: `demo-delivery-${index}`, userId: 'demo-user',
    items: [{ productName: variants[0].productName, productVariantId: variants[0].productVariantId, quantity: 2, unitPrice: variants[0].price }],
  }))
  const returns = ['PENDING', 'INSPECTING', 'REFUNDED'].map((orderReturnStatus, index) => ({ returnId: `DEMO-R${index + 1}`, customerName: orders[index].customerName, initialTime: orders[index].createdAt, reasonReturn: ['Giao nhầm biến thể', 'Sản phẩm bị móp', 'Đổi nhu cầu sử dụng'][index], orderReturnStatus, totalAmount: 185000, items: orders[index].items, shippingAddress: addresses[0].address }))
  const lookup = (items, key, id) => items.find(item => item[key] === id) || fail('Không tìm thấy dữ liệu Demo.', 404)
  const integer = (value, minimum = 0) => { if (!Number.isSafeInteger(value) || value < minimum) fail('Số lượng không hợp lệ.'); return value }
  const price = value => { if (!Number.isFinite(value) || value < 0) fail('Giá bán không hợp lệ.'); return value }
  const text = value => { if (typeof value !== 'string' || !value.trim() || value.length > 255) fail('Thông tin không hợp lệ.'); return value.trim() }
  function setActive(value) {
    active = value
    try { if (value) storage?.setItem(sessionKey, 'active'); else storage?.removeItem(sessionKey) } catch { storage = null }
  }
  function cartDetail() {
    const items = cart.map(item => { const variant = lookup(variants, 'productVariantId', item.productVariantId); return { ...item, productName: variant.productName, unitPrice: variant.price, lineTotal: variant.price * item.quantity, stockStatus: stockState(variant.quantityInStock) } })
    return { items, subtotal: items.reduce((sum, item) => sum + item.lineTotal, 0) }
  }
  function summary(body) {
    const { subtotal } = cartDetail()
    const discount = body.discountId ? lookup(discounts, 'discountId', body.discountId) : null
    const discountAmount = discount ? Math.round(subtotal * discount.discountValue / 100) : 0
    const shippingFee = subtotal ? 25000 : 0
    return { subtotal, discountAmount, shippingFee, total: subtotal - discountAmount + shippingFee }
  }
  function page(items, params, stateKey = 'state') {
    const pageNumber = integer(Number(params.get('page') || 0))
    const size = integer(Number(params.get('size') || 10), 1)
    if (size > 100) fail('Kích thước trang không hợp lệ.')
    const query = (params.get('query') || '').toLocaleLowerCase('vi-VN')
    const state = params.get('state') || params.get('filterBy')
    const filtered = items.filter(item => (!state || state === 'ALL_REQUESTS' || item[stateKey] === state) && (!query || Object.values(item).some(value => typeof value === 'string' && value.toLocaleLowerCase('vi-VN').includes(query))))
    return { content: filtered.slice(pageNumber * size, (pageNumber + 1) * size), totalElements: filtered.length, totalPages: Math.ceil(filtered.length / size), number: pageNumber, size }
  }
  function route(path, method, body, params) {
    let match
    if (method === 'GET') {
      if (path === '/auth/me') return account
      if (path === '/catalog') return page(variants.map(item => ({ ...item, variantId: item.productVariantId, description: item.productVariant })), params)
      if (path === '/carts') return cartDetail()
      if (path === '/addresses') return addresses
      if (path === '/payments') return payments
      if (path === '/discounts') return discounts
      if (path === '/shippers') return shippers
      if (path === '/inventories') return { totalInventoryValue: variants.reduce((sum, item) => sum + item.price * item.quantityInStock, 0), totalProductsInStock: variants.filter(item => item.quantityInStock > 0).length, totalProductsPrepareToOutOfStock: variants.filter(item => item.quantityInStock > 0 && item.quantityInStock < 10).length }
      if (path === '/inventories/products-in-stock') return page(variants.map(item => ({ ...item, productPrice: item.price, productDescription: item.productVariant, productStockQuantity: item.quantityInStock, productStockState: stockState(item.quantityInStock) })), params, 'productStockState')
      if (path === '/orders/returns/summary!') return { activeReturnCount: returns.filter(item => !['REFUNDED', 'REJECTED', 'RESTOCKED'].includes(item.orderReturnStatus)).length, awaitInspectionCount: returns.filter(item => item.orderReturnStatus === 'INSPECTING').length, averageCycleTime: '2 ngày', totalRefunds: returns.filter(item => item.orderReturnStatus === 'REFUNDED').reduce((sum, item) => sum + item.totalAmount, 0) }
      if (path === '/orders/returns') return page(returns, params, 'orderReturnStatus')
      if ((match = path.match(/^\/orders\/returns\/([^/]+)$/))) return lookup(returns, 'returnId', match[1])
      if (['/orders', '/orders/mine', '/shipper/orders'].includes(path)) return page(orders, params)
      if ((match = path.match(/^\/(?:orders(?:\/tracking)?|shipper\/orders)\/([^/]+)$/))) { const order = lookup(orders, 'orderId', match[1]); return { ...order, orderTrackingStatus: order.state } }
    }
    if (path === '/auth/logout' && method === 'POST') { setActive(false); cart = []; return null }
    if (path === '/carts/items' && method === 'POST') {
      const variant = lookup(variants, 'productVariantId', body.productVariantId)
      const quantity = integer(body.quantity, 1)
      const item = cart.find(item => item.productVariantId === variant.productVariantId)
      if ((item?.quantity || 0) + quantity > variant.quantityInStock) fail('Không đủ tồn kho Demo.')
      if (item) item.quantity += quantity
      else cart.push({ cartItemId: `demo-cart-${++sequence}`, productVariantId: variant.productVariantId, quantity })
      return cartDetail()
    }
    if ((match = path.match(/^\/carts\/items\/([^/]+)\/quantity$/)) && method === 'PATCH') {
      const item = lookup(cart, 'cartItemId', match[1])
      if (!Number.isSafeInteger(body.quantityChange)) fail('Thay đổi số lượng không hợp lệ.')
      const quantity = integer(item.quantity + body.quantityChange)
      if (quantity > lookup(variants, 'productVariantId', item.productVariantId).quantityInStock) fail('Không đủ tồn kho Demo.')
      if (quantity === 0) cart = cart.filter(row => row !== item)
      else item.quantity = quantity
      return cartDetail()
    }
    if (path === '/orders/orders/summary' && method === 'POST') return summary(body)
    if (path === '/orders' && method === 'POST') {
      if (!cart.length) fail('Giỏ hàng Demo đang trống.')
      const address = lookup(addresses, 'addressId', body.addressId)
      const payment = lookup(payments, 'paymentId', body.paymentId)
      const totals = summary(body)
      const items = cart.map(item => { const variant = lookup(variants, 'productVariantId', item.productVariantId); if (item.quantity > variant.quantityInStock) fail('Không đủ tồn kho Demo.'); return { productVariantId: variant.productVariantId, productName: variant.productName, quantity: item.quantity, unitPrice: variant.price } })
      const id = `DEMO-${++sequence + 1000}`
      const order = { ...orders[0], ...totals, id, orderId: id, state: 'PENDING', createdAt: new Date().toISOString(), customerName: account.userName, shippingAddress: address.address, shippingCity: address.city, paymentMethodInfo: payment.paymentMethod, items }
      items.forEach(item => { lookup(variants, 'productVariantId', item.productVariantId).quantityInStock -= item.quantity })
      orders.unshift(order); cart = []
      return order
    }
    if ((match = path.match(/^\/inventories\/([^/]+)\/quantity$/)) && method === 'PUT') { const quantity = integer(body.quantity); const item = lookup(variants, 'productVariantId', match[1]); item.quantityInStock = quantity; return item }
    if ((match = path.match(/^\/inventories\/productsInStock\/([^/]+)\/variants\/([^/]+)$/)) && method === 'PATCH') {
      const variant = lookup(variants, 'productVariantId', match[2])
      if (variant.productId !== match[1]) fail('Biến thể không thuộc sản phẩm.', 404)
      const product = lookup(products, 'productId', match[1])
      const updates = { productName: body.productName === undefined ? product.productName : text(body.productName), productType: body.productType === undefined ? product.productType : text(body.productType) }
      if (updates.productType.length > 100) fail('Loại sản phẩm tối đa 100 ký tự.')
      const newPrice = body.productPrice === undefined ? variant.price : price(body.productPrice)
      const quantity = body.quantityInStock === undefined ? variant.quantityInStock : integer(body.quantityInStock)
      if (body.productDescription !== undefined && (typeof body.productDescription !== 'string' || body.productDescription.length > 255)) fail('Mô tả không hợp lệ.')
      Object.assign(product, updates)
      variants.filter(item => item.productId === product.productId).forEach(item => Object.assign(item, updates))
      Object.assign(variant, { price: newPrice, quantityInStock: quantity, productVariant: body.productDescription ?? variant.productVariant })
      return variant
    }
    if (path === '/products' && method === 'POST') {
      const product = { productId: text(body.productId), productName: text(body.productName), productType: text(body.productType) }
      if (product.productType.length > 100) fail('Loại sản phẩm tối đa 100 ký tự.')
      if (products.some(item => item.productId === product.productId)) fail('Mã sản phẩm đã tồn tại.', 409)
      products.push(product); return product
    }
    if ((match = path.match(/^\/products\/([^/]+)\/variants$/)) && method === 'POST') {
      const product = lookup(products, 'productId', match[1])
      const variant = { ...product, productVariantId: `DEMO-BT-${++sequence}`, productVariant: text(body.productVariant), price: price(body.price), quantityInStock: 0 }
      variants.push(variant); return variant
    }
    if ((match = path.match(/^\/orders\/tracking\/([^/]+)\/state$/)) && method === 'PATCH') {
      const order = lookup(orders, 'orderId', match[1])
      if (!(transitions[order.state] || []).includes(body.state)) fail('Chuyển trạng thái không hợp lệ.')
      if (body.state === 'DELIVERY_FAILED' && !body.failureReason) fail('Cần lý do giao thất bại.')
      order.state = body.state; return order
    }
    if ((match = path.match(/^\/orders\/([^/]+)\/shipper$/)) && method === 'PATCH') {
      const order = lookup(orders, 'orderId', match[1])
      if (!['PROCESSING', 'DELIVERY_FAILED'].includes(order.state)) fail('Đơn chưa sẵn sàng phân công.')
      lookup(shippers, 'shipperId', body.shipperId); order.shipperId = body.shipperId; return order
    }
    if (method === 'POST' && ['/shipper/orders/receive', '/shipper/orders/delivered', '/shipper/orders/failed'].includes(path)) {
      const order = lookup(orders, 'orderId', body.orderId)
      if (path.endsWith('/receive')) { if (order.state !== 'PROCESSING') fail('Đơn chưa sẵn sàng nhận.'); order.state = 'SHIPPING' }
      else {
        if (order.state !== 'SHIPPING' || body.deliveryAttemptId !== order.deliveryAttemptId) fail('Lượt giao không hợp lệ.')
        if (body.customerName !== order.customerName || body.address !== order.shippingAddress) fail('Thông tin người nhận không khớp.')
        if (path.endsWith('/failed') && !body.failureReason) fail('Cần lý do giao thất bại.')
        order.state = path.endsWith('/failed') ? 'DELIVERY_FAILED' : 'DELIVERED'
      }
      return order
    }
    fail('Thao tác này chưa được hỗ trợ trong chế độ Demo.', 404)
  }
  return function handle(path, options = {}) {
    const url = new URL(path, 'http://demo.local')
    const method = (options.method || 'GET').toUpperCase()
    if (url.pathname === '/auth/login' && method === 'POST') {
      let credentials
      try { credentials = JSON.parse(options.body || '{}') } catch { setActive(false); return { handled: false, data: null } }
      if (credentials?.email === account.email && credentials?.password === 'Demo123!') { setActive(true); return { handled: true, data: copy(account) } }
      setActive(false); return { handled: false, data: null }
    }
    if (!active) return { handled: false, data: null }
    let body
    try { body = JSON.parse(options.body || '{}') } catch { fail('Dữ liệu Demo không hợp lệ.') }
    if (!body || typeof body !== 'object' || Array.isArray(body)) fail('Dữ liệu Demo không hợp lệ.')
    const data = route(decodeURI(url.pathname), method, body, url.searchParams)
    return { handled: true, data: copy(data) }
  }
}
