<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-card>
        <template #header>上传图片</template>

        <el-upload
          drag
          :show-file-list="false"
          :http-request="doUpload"
          :before-upload="beforeUpload"
          accept="image/jpeg,image/png,image/webp,image/bmp"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="el-upload__text">把图片拖到这里，或<em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">
              支持 jpg / png / webp / bmp，单张不超过 10MB
            </div>
          </template>
        </el-upload>

        <el-alert
          v-if="loading"
          title="正在识别，请稍候（首次调用需加载模型，约 5-8 秒）"
          type="info"
          :closable="false"
          show-icon
          style="margin-top:16px"
        />
      </el-card>
    </el-col>

    <el-col :span="12">
      <el-card>
        <template #header>识别结果</template>

        <el-empty v-if="!result" description="还没有识别结果" />

        <div v-else>
          <div class="preview">
            <img :src="result.url" alt="预览" />
          </div>

          <div class="top">
            <span class="label">最终结果</span>
            <span class="name">{{ result.topName || '未识别' }}</span>
            <span class="conf">{{ ((result.topConfidence || 0) * 100).toFixed(1) }}%</span>
          </div>

          <el-divider content-position="left">前 5 个候选</el-divider>

          <div v-for="c in result.candidates" :key="c.rank" class="candidate">
            <span class="rank">{{ c.rank }}</span>
            <span class="cname">{{ c.nameCn || c.nameEn }}</span>
            <el-progress
              :percentage="Math.round(c.confidence * 100)"
              :stroke-width="12"
              style="flex:1"
            />
          </div>

          <div class="meta">
            模型：{{ result.modelVersion }} · 耗时：{{ result.costMs }} ms
            <el-link type="primary" style="margin-left:12px" @click="$router.push(`/detail/${result.id}`)">
              查看详情
            </el-link>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '../api'

const loading = ref(false)
const result = ref(null)

function beforeUpload(file) {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过 10MB')
    return false
  }
  return true
}

async function doUpload({ file }) {
  loading.value = true
  try {
    result.value = await api.uploadImage(file)
    ElMessage.success('识别完成')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.upload-icon { font-size: 52px; color: #c0c4cc; }
.preview { text-align: center; margin-bottom: 16px; }
.preview img { max-width: 100%; max-height: 220px; border-radius: 6px; }
.top { display: flex; align-items: baseline; gap: 10px; }
.label { color: #909399; font-size: 13px; }
.name { font-size: 22px; font-weight: 600; color: #303133; }
.conf { font-size: 16px; color: #4364f7; font-weight: 600; }
.candidate { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.rank {
  width: 20px; height: 20px; line-height: 20px; text-align: center;
  background: #f0f2f5; border-radius: 50%; font-size: 12px; color: #606266;
}
.cname { width: 90px; font-size: 13px; color: #303133; }
.meta { margin-top: 14px; font-size: 12px; color: #909399; }
</style>
