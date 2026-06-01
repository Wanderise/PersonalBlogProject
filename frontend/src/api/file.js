import request from "@/api/request"

export function getUploadUrl(data) {
  return request({
    url: '/file/upload/url',
    method: 'POST',
    data
  })
}

export function getDownloadUrl(objectKey) {
  return request({
    url: '/file/download/url',
    method: 'GET',
    params: { objectKey }
  })
}
