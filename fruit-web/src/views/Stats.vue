<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="head">
        <span>各类别识别数量统计</span>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </template>

    <el-empty v-if="!loading && !rows.length" description="还没有识别记录" />
    <div v-show="rows.length" ref="chartRef" class="chart"></div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import * as api from '../api'

const chartRef = ref()
const rows = ref([])
const loading = ref(false)
let chart = null

async function load() {
  loading.value = true
  try {
    rows.value = await api.statsByCategory()
    await nextTick()
    render()
  } finally {
    loading.value = false
  }
}

function render() {
  if (!chartRef.value || !rows.value.length) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 30, top: 30, bottom: 60 },
    xAxis: {
      type: 'category',
      data: rows.value.map((r) => r.nameCn),
      axisLabel: { rotate: 30 }
    },
    yAxis: { type: 'value', name: '数量' },
    series: [{
      type: 'bar',
      data: rows.value.map((r) => r.cnt),
      barMaxWidth: 46,
      itemStyle: { color: '#4364f7', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  })
}

function onResize() { chart?.resize() }

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.chart { height: 420px; }
</style>
