<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">🍎 水果识别</div>
      <el-menu :default-active="$route.path" router class="menu">
        <el-menu-item index="/upload">
          <el-icon><UploadFilled /></el-icon><span>图片识别</span>
        </el-menu-item>
        <el-menu-item index="/list">
          <el-icon><Picture /></el-icon><span>识别记录</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><DataAnalysis /></el-icon><span>数据统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title || '' }}</span>
        <el-dropdown @command="onCommand">
          <span class="user">
            {{ store.user?.nickname || store.user?.username || '用户' }}
            <el-tag v-if="store.isAdmin()" size="small" type="danger" style="margin-left:6px">管理员</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()

function onCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: #fff; border-right: 1px solid #e4e7ed; }
.logo {
  height: 60px; line-height: 60px; text-align: center;
  font-size: 17px; font-weight: 600; color: #4364f7;
  border-bottom: 1px solid #e4e7ed;
}
.menu { border-right: none; }
.header {
  background: #fff; border-bottom: 1px solid #e4e7ed;
  display: flex; align-items: center; justify-content: space-between;
}
.page-title { font-size: 16px; font-weight: 600; color: #303133; }
.user { cursor: pointer; display: flex; align-items: center; gap: 4px; color: #606266; }
.main { padding: 20px; }
</style>
