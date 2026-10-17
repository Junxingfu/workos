<script setup lang="ts">
import { computed, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import PageHeader from '@/components/PageHeader.vue'
import { applyTaskWorkbook, exportTaskWorkbook, previewTaskWorkbook, fail, type ImportPreview, type ImportResult } from '@/api'
import { notifyCaptureCreated } from '@/composables/useCaptureRefresh'
const busy = ref(false)
const preview = ref<ImportPreview | null>(null)
const result = ref<ImportResult | null>(null)
const filename = ref('')
const error = ref('')
const input = ref<HTMLInputElement>()
const filter = ref('全部')
const page = ref(1)
const rows = computed(() => preview.value?.rows.filter(r => filter.value === '全部' || r.action === filter.value) ?? [])
const visibleRows = computed(() => rows.value.slice((page.value - 1) * 30, page.value * 30))
const counts = computed(() => ['新增', '修改', '未变化', '错误'].map(action => ({ action, count: preview.value?.rows.filter(r => r.action === action).length ?? 0 })))
async function download(template: boolean) {
  busy.value = true
  try {
    const blob = await exportTaskWorkbook(template)
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = template ? 'WorkOS-任务录入模板.xlsx' : 'WorkOS-任务.xlsx'; anchor.click()
    setTimeout(() => URL.revokeObjectURL(url), 10000)
  } catch (e) { fail(e) } finally { busy.value = false }
}
async function choose(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  filename.value = file.name; preview.value = null; result.value = null; error.value = ''; page.value = 1; filter.value = '全部'
  if (file.size > 5 * 1024 * 1024) { error.value = '文件不能超过 5 MB'; return }
  busy.value = true
  try { preview.value = await previewTaskWorkbook(file) } catch (e) { error.value = e instanceof Error ? e.message : '读取失败' } finally { busy.value = false; if (input.value) input.value.value = '' }
}
async function apply() {
  if (!preview.value?.token || busy.value) return
  busy.value = true; error.value = ''
  try {
    result.value = await applyTaskWorkbook(preview.value.token)
    preview.value = null; notifyCaptureCreated(); window.dispatchEvent(new Event('workos:attention-changed'))
    message.success('任务已更新')
  } catch (e) { error.value = e instanceof Error ? e.message : '提交失败'; } finally { busy.value = false }
}
onBeforeRouteLeave(() => {
  if (busy.value) return false
  if (!preview.value?.canApply) return true
  return new Promise<boolean>(resolve => Modal.confirm({ title: '离开导入预览？', content: '表格尚未提交，任务没有变化。下次进入需要重新选择文件。', okText: '离开', cancelText: '继续核对', onOk: () => resolve(true), onCancel: () => resolve(false) }))
})
</script>
<template>
  <PageHeader title="Excel 批量维护" sub="在表格里录入或修改任务，核对变化后一次保存。" />
  <section class="exchange-guide">
    <h2>1. 准备表格</h2>
    <p>修改已有任务先导出，保留事项 ID、版本和创建标识。新增任务用模板，前三列留空。</p>
    <a-space wrap><a-button :disabled="busy" @click="download(false)">导出任务</a-button><a-button :disabled="busy" @click="download(true)">下载录入模板</a-button></a-space>
    <p class="meta-text">空白表示保留原值；填写 [清空] 移除可选信息。删除表格行不会删除任务。项目名称或 ID 均可，参考工作簿内的“填写说明”和“项目参考”。</p>
  </section>
  <section class="exchange-guide">
    <h2>2. 选择表格并核对</h2>
    <input ref="input" type="file" accept=".xlsx" aria-label="选择 Excel 文件" :disabled="busy" @change="choose" />
    <p class="meta-text">仅 .xlsx，最多 2,000 行 / 5 MB。预览不会修改任务；任何一行失败，整批都不会提交。</p>
    <a-spin v-if="busy" tip="处理中…" />
    <a-alert v-if="error" type="error" :message="error" show-icon />
    <a-alert v-if="result" type="success" :message="`已保存：新增 ${result.created} 条，修改 ${result.updated} 条，未变化 ${result.unchanged} 条`" description="再次导入同一份表格不会重复创建任务。继续修改请重新导出最新版本。" show-icon />
    <template v-if="preview">
      <p>{{ filename }} · {{ preview.message }}</p>
      <a-space wrap><a-button :type="filter === '全部' ? 'primary' : 'default'" @click="filter = '全部'; page = 1">全部 {{ preview.rows.length }}</a-button><a-button v-for="count in counts" :key="count.action" :type="filter === count.action ? 'primary' : 'default'" @click="filter = count.action; page = 1">{{ count.action }} {{ count.count }}</a-button></a-space>
      <div class="preview-list">
        <article v-for="row in visibleRows" :key="row.row">
          <div class="row-title"><span>第 {{ row.row }} 行 · {{ row.action }}</span><strong>{{ row.title || '未填写标题' }}</strong></div>
          <p v-if="row.error" class="error-text">{{ row.error }}</p>
          <dl v-else-if="row.changes.length"><div v-for="change in row.changes" :key="change.field"><dt>{{ change.field }}</dt><dd><span>{{ change.before || '空' }}</span><span aria-label="改为"> → </span><b>{{ change.after || '清空' }}</b></dd></div></dl>
        </article>
      </div>
      <a-pagination v-if="rows.length > 30" v-model:current="page" :total="rows.length" :page-size="30" :show-size-changer="false" />
      <div v-if="preview.canApply" class="commit-bar"><span>核对后统一保存，桌面版会先保留一份备份。</span><a-button type="primary" :loading="busy" @click="apply">确认并保存 {{ counts[0]!.count + counts[1]!.count }} 条变化</a-button></div>
    </template>
  </section>
</template>
<style scoped>
.exchange-guide { padding: 20px 0; border-bottom: 1px solid var(--color-border); }
h2 { font-size: 15px; margin: 0 0 12px; }
p { line-height: 1.7; }
.preview-list { margin: 16px 0; }
article { padding: 14px 0; border-top: 1px solid var(--color-border); }
.row-title { display: flex; gap: 16px; flex-wrap: wrap; overflow-wrap: anywhere; }
.row-title > span { color: var(--color-text-secondary); }
dl { margin: 8px 0 0; } dl > div { display: flex; gap: 12px; margin: 4px 0; } dt { width: 110px; flex: none; color: var(--color-text-secondary); } dd { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; min-width: 0; }
.error-text { color: var(--color-danger, #dc2626); }
.commit-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; padding: 16px 0; position: sticky; bottom: 0; background: var(--color-surface, white); border-top: 1px solid var(--color-border); }
</style>
