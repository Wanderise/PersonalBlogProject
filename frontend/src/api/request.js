import axios from "axios";

const service = axios.create({
    baseURL: 'http://localhost:8080/',
    timeout: 5000
});

service.interceptors.request.use(
    (config) =>{
        const token = localStorage.getItem('token');
        if(token){
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        console.log('正在请求',config.url);
        return config;
    },
    (error) =>{
        return Promise.reject(error);
    }
)

// 3. 响应拦截器
service.interceptors.response.use(
    (response) => {
        // 这里的 response 是 Axios 包装后的对象
        // 我们通常直接返回后端返回的真实业务数据 (response.data)
        const res = response.data;

        // 统一处理业务逻辑错误（假设后端定义 200 为成功）
        if (res.code !== 200) {
            return Promise.reject(new Error(res.msg || 'Error'));
        }

        return res;
    },
    (error) => {
        // 统一处理 HTTP 状态码错误
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    // Token 过期，跳转到登录页
                    console.error('未授权，请重新登录');
                    break;
                case 500:
                    console.error('服务器内部错误');
                    break;
            }
        }
        return Promise.reject(error);
    }
);

export default service;