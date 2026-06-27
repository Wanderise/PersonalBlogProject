import { createApp } from 'vue'
import App from './App.vue'
import 'element-plus/dist/index.css'
import './styles/global.css'

// markdown editor
import VueMarkdownEditor from '@kangc/v-md-editor';
import '@kangc/v-md-editor/lib/style/base-editor.css';
import '@kangc/v-md-editor/lib/style/preview.css';
import vuepressTheme from '@kangc/v-md-editor/lib/theme/vuepress.js';
import '@kangc/v-md-editor/lib/theme/style/vuepress.css';
import Prism from 'prismjs';

// router
import router from './router.js'

VueMarkdownEditor.use(vuepressTheme, {
    Prism,
});


const app = createApp(App)
app.use(VueMarkdownEditor);
app.use(router);
app.mount('#app')
