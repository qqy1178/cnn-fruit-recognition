<template>
  <div class="login-page">
    <el-card class="box">
      <h2 class="title">水果识别系统</h2>
      <p class="sub">基于卷积神经网络</p>

      <el-form :model="form" :rules="rules" ref="formRef" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
                    :prefix-icon="Lock" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-form-item v-if="isRegister" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（可留空）" />
        </el-form-item>

        <el-button type="primary" class="submit" :loading="loading" @click="submit">
          {{ isRegister ? '注 册' : '登 录' }}
        </el-button>
      </el-form>

      <div class="switch">
        <el-link type="primary" @click="toggle">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import * as api from '../api'

const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const isRegister = ref(false)

const form = reactive({ username: '', password: '', nickname: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ]
}

function toggle() {
  isRegister.value = !isRegister.value
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (isRegister.value) {
      await api.register(form)
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
    } else {
      await store.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      router.push('/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6fb1fc 0%, #4364f7 100%);
}
.box { width: 380px; padding: 12px 8px; }
.title { text-align: center; margin-bottom: 4px; color: #303133; }
.sub { text-align: center; color: #909399; font-size: 13px; margin-bottom: 24px; }
.submit { width: 100%; }
.switch { text-align: center; margin-top: 14px; }
</style>
