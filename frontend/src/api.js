import { createDemo } from './demo.js'

let demo

export const states = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PROCESSING: 'Đang xử lý', SHIPPING: 'Đang giao', DELIVERED: 'Đã giao', DELIVERY_FAILED: 'Giao thất bại', CANCELLED: 'Đã hủy', IN_STOCK: 'Còn hàng', LIMITED_STOCK: 'Sắp hết hàng', OUT_OF_STOCK: 'Hết hàng', REFUNDED: 'Đã hoàn tiền', RESTOCKED: 'Đã nhập lại', INSPECTING: 'Đang kiểm tra', WAREHOUSE_RECEIVED: 'Đã về kho', IN_TRANSIT: 'Đang vận chuyển', REJECTED: 'Từ chối' }
export const transitions = { PENDING: ['CONFIRMED', 'PROCESSING', 'CANCELLED'], CONFIRMED: ['PROCESSING', 'CANCELLED'], PROCESSING: ['SHIPPING', 'CANCELLED'], SHIPPING: ['DELIVERED', 'DELIVERY_FAILED'], DELIVERY_FAILED: ['SHIPPING'] }
export const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
export const date = value => value ? new Date(value).toLocaleString('vi-VN') : '—'
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
    const error = new Error(payload?.message || (response.status === 401 ? 'Phiên đăng nhập đã hết hạn.' : response.status === 403 ? 'Bạn không có quyền thực hiện thao tác này.' : 'Không thể kết nối dịch vụ. Vui lòng thử lại.'))
    Object.assign(error, { status: response.status, fields: payload?.metadata })
    throw error
  }
  return payload?.data
}
export const send = (path, method, body) => api(path, { method, body: JSON.stringify(body) })
