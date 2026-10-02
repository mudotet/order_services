import { createDemo } from './demo.js'

let demo

export const states = { PENDING: 'Pending confirmation', CONFIRMED: 'Confirmed', PROCESSING: 'Processing', SHIPPING: 'Out for delivery', DELIVERED: 'Delivered', DELIVERY_FAILED: 'Delivery failed', CANCELLED: 'Cancelled', IN_STOCK: 'In stock', LIMITED_STOCK: 'Low stock', OUT_OF_STOCK: 'Out of stock', REFUNDED: 'Refunded', RESTOCKED: 'Restocked', INSPECTING: 'Inspecting', WAREHOUSE_RECEIVED: 'Received at warehouse', IN_TRANSIT: 'In transit', REJECTED: 'Rejected' }
export const transitions = { PENDING: ['CONFIRMED', 'PROCESSING', 'CANCELLED'], CONFIRMED: ['PROCESSING', 'CANCELLED'], PROCESSING: ['SHIPPING', 'CANCELLED'], SHIPPING: ['DELIVERED', 'DELIVERY_FAILED'], DELIVERY_FAILED: ['SHIPPING'] }
export const money = value => new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
export const date = value => value ? new Date(value).toLocaleString('en-GB') : '—'
export const list = data => Array.isArray(data) ? data : data?.content || []
export async function api(path, options = {}) {
  if (import.meta.env?.DEV) {
    if (!demo) {
      let storage = null
      try { storage = globalThis.sessionStorage } catch { storage = null }
      demo = createDemo(storage)
    }
    const result = demo(path, options)
    if (result.handled) return result.data
  }
  const response = await fetch(`/api${path}`, { credentials: 'include', ...options, headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } })
  const payload = await response.json().catch(() => null)
  if (!response.ok || (payload?.code && payload.code !== 'SUCCESS') || payload?.data === false) {
    const error = new Error(payload?.message || (response.status === 401 ? 'Your session has expired.' : response.status === 403 ? 'You do not have permission to perform this action.' : 'Unable to connect to the service. Please try again.'))
    Object.assign(error, { status: response.status, fields: payload?.metadata })
    throw error
  }
  return payload?.data
}
export const send = (path, method, body) => api(path, { method, body: JSON.stringify(body) })
