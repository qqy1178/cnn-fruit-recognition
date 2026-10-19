<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>识别记录</span>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="图片" width="100">
        <template #default="{ row }">
          <el-image :src="row.url" fit="cover" class="thumb"
                    :preview-src-list="[row.url]" preview-teleported />
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column label="识别结果" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.topName" type="success">{{ row.topName }}</el-tag>
          <span v-else class="muted">未识别</span>
        </template>
      </el-table-column>
      <el-table-column label="置信度" width="120">
        <template #default="{ row }">
          <span v-if="row.topConfidence != null">
            {{ (row.topConfidence * 100).toFixed(1) }}%
          </span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="uploadTime" label="上传时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/detail/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :current-page="page"
      :page-size="size"
      @current-change="onPageChange"
    />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as api from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await api.listImages(page.value, size.value)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  load()
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.thumb { width: 64px; height: 64px; border-radius: 4px; }
.muted { color: #c0c4cc; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
