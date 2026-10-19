<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="head">
        <span>识别详情</span>
        <el-button size="small" @click="$router.back()">返回</el-button>
      </div>
    </template>

    <el-row v-if="detail" :gutter="24">
      <el-col :span="12">
        <el-image :src="detail.url" fit="contain" class="big"
                  :preview-src-list="[detail.url]" preview-teleported />
        <div class="info">
          <div>文件名：{{ detail.fileName }}</div>
          <div>大小：{{ ((detail.fileSize || 0) / 1024).toFixed(1) }} KB</div>
          <div>上传时间：{{ detail.uploadTime }}</div>
          <div>模型版本：{{ detail.modelVersion || '—' }}</div>
          <div>识别耗时：{{ detail.costMs != null ? detail.costMs + ' ms' : '—' }}</div>
        </div>
      </el-col>

      <el-col :span="12">
        <el-result icon="success" :title="detail.topName || '未识别'"
                   :sub-title="`置信度 ${((detail.topConfidence || 0) * 100).toFixed(1)}%`" />

        <el-divider content-position="left">前 5 个候选</el-divider>

        <el-table :data="detail.candidates" size="small">
          <el-table-column prop="rank" label="#" width="50" />
          <el-table-column label="类别" width="120">
            <template #default="{ row }">{{ row.nameCn || row.nameEn }}</template>
          </el-table-column>
          <el-table-column label="置信度">
            <template #default="{ row }">
              <el-progress :percentage="Math.round(row.confidence * 100)" :stroke-width="10" />
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <el-empty v-else-if="!loading" description="没有找到这条记录" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as api from '../api'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await api.getImageDetail(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.big { width: 100%; max-height: 340px; border-radius: 6px; background: #fafafa; }
.info { margin-top: 14px; font-size: 13px; color: #606266; line-height: 1.9; }
</style>
