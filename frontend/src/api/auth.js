import request from "@/api/request.js";

export function login(data) {
    return request({
        url: '/user/login',
        method: 'POST',
        data
    })
}

export function register(data) {
    return request({
        url: '/user/register',
        method: 'POST',
        data
    })
}

export function getUserInfo() {
    return request({
        url: '/user/info',
        method: 'GET'
    })
}

export function updateUserInfo(data) {
    return request({
        url: '/user/info',
        method: 'PUT',
        data
    })
}

export function updateAvatar(objectKey) {
    return request({
        url: '/user/avatar',
        method: 'PUT',
        data: { objectKey }
    })
}

export function updatePassword(data) {
    return request({
        url: '/user/password',
        method: 'PUT',
        data
    })
}
