<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { backupDesktop, fail, fetchDesktopStorage, importDesktop, restoreDesktop, type DesktopStorageInfo } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
const info = ref<DesktopStorageInfo | null>(null)
const busy = ref(false)
const error = ref('')
const database = ref('workos')
function backupLabel(name: string) {
  const kind = name.startsWith('auto-') ? '自动备份' : name.startsWith('manual-') ? '手动备份' : name.startsWith('before-import-') ? '导入前副本' : '恢复前副本'
  const date = name.match(/(\d{4}-\d{2}-\d{2})T(\d{2})-(\d{2})-(\d{2})(?:\.\d+)?Z/)
  return date ? `${kind} · ${dayjs(`${date[1]}T${date[2]}:${date[3]}:${date[4]}Z`).format('YYYY-MM-DD HH:mm:ss')}` : name
}
async function load() {
  error.value = ''
  try { info.value = await fetchDesktopStorage() } catch (e) { error.value = e instanceof Error ? e.message : '读取失败' }
}
async function run(action: () => Promise<unknown>, tip: string) {
  if (busy.value) return
  busy.value = true
  try { await action(); message.success(tip); await load(); window.dispatchEvent(new Event('workos:inbox-changed')) }
  catch (e) { fail(e) } finally { busy.value = false }
}
function restore(name: string) {
  Modal.confirm({ title: '恢复这份备份？', content: '当前数据会被备份中的内容替换。恢复前会自动保存一份当前数据，以便撤回。', okText: '备份当前并恢复', cancelText: '继续使用当前数据', onOk: () => run(() => restoreDesktop(name), '已恢复，其他页面将读取恢复后的数据') })
}
function migrate() {
  Modal.confirm({ title: '导入原来的工作记录？', content: '仅复制原数据库，不修改原数据。导入后桌面版独立保存，后续与网页版的修改不会自动同步。', okText: '开始导入', cancelText: '暂不导入', onOk: () => run(() => importDesktop(database.value), '导入完成，可以开始使用') })
}
onMounted(load)
</script>

<template>
  <PageHeader title="数据与备份" sub="数据保存在这台电脑，安装新版本不会覆盖工作记录。">
    <template #actions><a-button type="primary" :loading="busy" @click="run(backupDesktop, '备份已保存')">立即备份</a-button></template>
  </PageHeader>
  <a-alert v-if="error" type="error" :message="error" show-icon><template #action><a-button @click="load">重试</a-button></template></a-alert>
  <template v-if="info">
    <section class="section">
      <h2 class="section-title">本地工作区</h2>
      <p>{{ info.counts.project }} 个项目 · {{ info.counts.item }} 条事项 · {{ info.counts.work_log }} 条工作记录 · {{ info.counts.daily }} 份日报</p>
      <p class="data-path">{{ info.directory }}</p>
      <p class="meta-text">每天首次运行及运行期间跨日自动备份，保留最近 14 份自动备份。手动备份单独保留；重要备份可从应用菜单打开数据目录后另存到其他设备。</p>
    </section>
    <section v-if="info.canImport" class="section">
      <h2 class="section-title">从网页版迁移</h2>
      <p>已有工作记录时，保持原来的本地数据库运行，导入一次即可。导入完成后，日常使用不再依赖 Docker。</p>
      <a-select v-model:value="database" aria-label="导入来源" :disabled="busy" style="width: 210px" :options="[{ value: 'workos', label: '原工作库' }, { value: 'workos_usability_20261011', label: '原示例工作区' }]" />
      <a-button :disabled="busy" style="margin-left: 8px" @click="migrate">导入旧数据</a-button>
      <p class="meta-text">也可以直接到“今天”开始记录，使用全新的工作区。</p>
    </section>
    <section class="section">
      <h2 class="section-title">可恢复的备份</h2>
      <p v-if="!info.backups.length">尚无备份，点击“立即备份”保存当前数据。</p>
      <div v-for="name in info.backups" :key="name" class="backup-row">
        <span :title="name">{{ backupLabel(name) }}</span><a-button size="small" :disabled="busy" @click="restore(name)">恢复此备份</a-button>
      </div>
    </section>
  </template>
  <a-skeleton v-else-if="!error" active />
</template>

<style scoped>
.section { margin-top: 24px; }
.data-path { overflow-wrap: anywhere; color: var(--color-text-secondary); }
.backup-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; border-bottom: 1px solid var(--color-border); padding: 12px 0; }
.backup-row span { overflow-wrap: anywhere; min-width: 0; }
.backup-row button { flex-shrink: 0; }
</style>
