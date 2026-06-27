import request from "@/api/request"

export function getUploadUrl(data) {
  return request({
    url: '/file/upload/url',
    method: 'POST',
    data
  })
}

export function uploadObject(data) {
  return request({
    url: '/file/upload',
    method: 'POST',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  })
}

export function getDownloadUrl(objectKey) {
  return request({
    url: '/file/download/url',
    method: 'GET',
    params: { objectKey }
  })
}
