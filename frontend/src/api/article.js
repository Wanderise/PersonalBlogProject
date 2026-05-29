import request from '@/api/request.js'

export function uploadArticleRequest(data) {
  return request({
    url: '/article/add',
    method: 'POST',
    data
  })
}

export function getArticleList(params) {
  return request({
    url: '/article/list',
    method: 'GET',
    params
  })
}

export function getArticleById(id) {
  return request({
    url: `/article/${id}`,
    method: 'GET'
  })
}

export function updateArticle(id, data) {
  return request({
    url: `/article/${id}`,
    method: 'PUT',
    data
  })
}

export function deleteArticle(id) {
  return request({
    url: `/article/${id}`,
    method: 'DELETE'
  })
}

export function getMyArticles(params) {
  return request({
    url: '/article/my',
    method: 'GET',
    params
  })
}
