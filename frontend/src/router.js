import { createRouter, createWebHistory } from 'vue-router'

import MdEditor from '@/components/user/mdeditor/MdEditor.vue'
import Test from '@/components/user/homeview/test.vue'
import MainList from '@/components/user/articlelist/MainList.vue'
import MyArticles from '@/components/user/articlelist/MyArticles.vue'
import ArticleDetail from '@/components/user/articlelist/ArticleDetail.vue'
import User from '@/components/user/user.vue'
import LoginView from '@/components/user/auth/LoginView.vue'
import RegisterView from '@/components/user/auth/RegisterView.vue'
import UserProfile from '@/components/user/auth/UserProfile.vue'
import AiChatPage from '@/components/user/ai/AiChatPage.vue'

const routes = [
  { path: '/', redirect: '/User/1/home' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/register', name: 'Register', component: RegisterView },
  { path: '/article/:id', name: 'ArticleDetail', component: ArticleDetail },
  { path: '/editor', name: 'Editor', component: MdEditor, meta: { requiresAuth: true } },
  { path: '/ai', name: 'AiChat', component: AiChatPage, meta: { requiresAuth: true } },
  {
    path: '/User/:id',
    component: User,
    meta: { requiresAuth: true },
    children: [
      { path: 'home', name: 'Home', component: Test },
      { path: 'list', name: 'List', component: MainList },
      { path: 'articles', name: 'MyArticles', component: MyArticles },
      { path: 'profile', name: 'Profile', component: UserProfile },
      { path: '', name: 'Default', component: Test }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.matched.some((r) => r.meta.requiresAuth)) {
    next(token ? undefined : { name: 'Login' })
  } else if ((to.name === 'Login' || to.name === 'Register') && token) {
    next({ path: '/User/1/home' })
  } else {
    next()
  }
})

export default router
