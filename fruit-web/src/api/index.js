import request from './request'

export const login = (data) => request.post('/auth/login', data)

export const register = (data) => request.post('/auth/register', data)

export const uploadImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/image/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const listImages = (page = 1, size = 10) =>
  request.get('/image/list', { params: { page, size } })

export const getImageDetail = (id) => request.get(`/image/${id}`)

export const statsByCategory = () => request.get('/stats/category')
