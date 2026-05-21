import { createRouter, createWebHistory } from "vue-router";

import MdEditor from "@/components/user/mdeditor/MdEditor.vue";
import Test from "@/components/user/homeview/test.vue";
import List from "@/components/user/articlelist/MainList.vue"
import User from "@/components/user/user.vue"
import LoginView from "@/components/user/auth/LoginView.vue"
import RegisterView from "@/components/user/auth/RegisterView.vue"
import UserProfile from "@/components/user/auth/UserProfile.vue"

const routes = [
    { path: "/", redirect: "/User/1/home" },
    { path: "/login", name: "Login", component: LoginView },
    { path: "/register", name: "Register", component: RegisterView },
    { path: "/User/:id", component: User, meta: { requiresAuth: true }, children: [
            { path: "home", name: "Home", component: Test },
            { path: "editor", name: "Editor", component: MdEditor },
            { path: "list", name: "List", component: List },
            { path: "profile", name: "Profile", component: UserProfile },
            { path: "", name: "Default", component: Test }
        ]},
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.matched.some(record => record.meta.requiresAuth)) {
        if (!token) {
            next({ name: 'Login' })
        } else {
            next()
        }
    } else if ((to.name === 'Login' || to.name === 'Register') && token) {
        next({ path: '/User/1/home' })
    } else {
        next()
    }
})

export default router
