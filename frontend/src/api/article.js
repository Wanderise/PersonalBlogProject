import request from "@/api/request.js";

export function uploadArticleRequest(data){
    return request({
        url: '/article/add',
        method: 'POST',
        data
    })
}

export function getArticleList(){
    return request({
        url: '/article/list',
        method: 'GET'
    })
}

export function getArticleByTitle(title){
    return request({
        url: '/article/list',
        method: 'POST',
        title
    })
}