import { h, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fail } from '@/api'
import type { Item } from '@/types'

export interface ListActionsOptions {
  /** Enter 键打开当前选中项 */
  onOpen?: (item: Item) => void
  /**
   * 方向键是否也用来选择。
   * 只在页面本身不会滚动时开启：长列表上劫持上下键会顶掉滚动。
   */
  arrows?: boolean
  /** 页面自己的数字键动作，返回 true 表示已处理 */
  onDigit?: (key: string, item: Item) => boolean
}

export interface RunOptions {
  tip?: string
  /** 给不可逆动作一个反悔窗口 */
  undo?: () => Promise<void>
  /** 就地编辑（改归属/优先级）时留在本行；会让事项离开列表的动作才交给邻居 */
  keepSelection?: boolean
}

/**
 * 事项列表的动作约定（今天 / 全部事项共用，后续「待整理」也走这里）：
 *
 * 1. loading 只挂在被点的那个控件上，不整页变灰；
 * 2. 写操作后仍然重新 GET（不做乐观更新），但刷新后把选中态和焦点交给邻居，
 *    用户不需要重新找自己刚才在哪一条。
 * 3. j / k 选择、Enter 打开、Esc 取消选中；误点有代价的动作给撤销入口。
 *
 * 这不是视觉抽象，是同一套行为在多个列表里的唯一实现。
 */
export function useListActions(
  rows: () => Item[],
  reload: () => Promise<void>,
  options: ListActionsOptions = {}
) {
  const selectedId = ref<string | null>(null)
  const pending = ref<Record<string, string>>({})

  function select(item: Item) {
    selectedId.value = item.id
  }

  function pendingActionFor(item: Item): string | null {
    return pending.value[item.id] ?? null
  }

  /** 按页面渲染顺序找邻居：用于动作后把「你在这里」交给下一条 */
  function neighborOf(id: string): string | null {
    const ids = rows().map((item) => item.id)
    const index = ids.indexOf(id)
    if (index < 0) return null
    return ids[index + 1] ?? ids[index - 1] ?? null
  }

  async function keepPlace(nextId: string | null) {
    selectedId.value = nextId
    await nextTick()
    if (nextId) document.querySelector<HTMLElement>(`[data-item-id="${nextId}"]`)?.focus()
  }

  /**
   * 统一动作包装。默认把选中态交给邻居（因为多数动作会让事项离开当前列表）；
   * 就地编辑类动作传 keepSelection 留在本行。
   */
  async function run(item: Item, action: string, task: () => Promise<unknown>, opts: RunOptions = {}) {
    if (pending.value[item.id]) return false
    const { tip = '', undo, keepSelection = false } = opts
    const nextId = keepSelection ? item.id : neighborOf(item.id)
    pending.value[item.id] = action
    try {
      await task()
      if (tip) {
        if (undo) {
          message.success(
            h('span', [
              `${tip} `,
              h(
                'a',
                {
                  onClick: async () => {
                    try {
                      await undo()
                      await reload()
                    } catch (e) {
                      fail(e)
                    }
                  }
                },
                '撤销'
              )
            ]),
            6
          )
        } else {
          message.success(tip)
        }
      }
      await reload()
      window.dispatchEvent(new Event('workos:inbox-changed'))
      await keepPlace(rows().some((row) => row.id === item.id) ? item.id : nextId)
      return true
    } catch (e) {
      fail(e)
      return false
    } finally {
      delete pending.value[item.id]
    }
  }

  /* ---------------- 键盘：j / k 选择、Enter 打开、Esc 取消 ---------------- */

  function indexOfSelected(): number {
    return rows().map((item) => item.id).indexOf(selectedId.value ?? '')
  }

  function moveSelection(offset: number) {
    const list = rows()
    if (!list.length) return
    const current = indexOfSelected()
    const next =
      current < 0
        ? offset > 0
          ? 0
          : list.length - 1
        : Math.min(Math.max(current + offset, 0), list.length - 1)
    void keepPlace(list[next].id)
  }

  function onKeydown(event: KeyboardEvent) {
    const target = event.target as HTMLElement | null
    if (event.defaultPrevented || event.isComposing) return
    if (document.querySelector('.ant-modal-wrap:not([style*="display: none"]), .ant-drawer-open')) return
    if (target?.closest('input, textarea, select, button, a, [contenteditable="true"], [role="combobox"], [role="textbox"]')) return
    if (event.metaKey || event.ctrlKey || event.altKey) return

    const list = rows()
    if (!list.length) return
    const current = list[indexOfSelected()] ?? null

    switch (event.key) {
      case 'j':
        moveSelection(1)
        event.preventDefault()
        return
      case 'k':
        moveSelection(-1)
        event.preventDefault()
        return
      case 'ArrowDown':
        if (!options.arrows) return
        moveSelection(1)
        event.preventDefault()
        return
      case 'ArrowUp':
        if (!options.arrows) return
        moveSelection(-1)
        event.preventDefault()
        return
      case 'Enter':
        if (!current || !options.onOpen) return
        options.onOpen(current)
        event.preventDefault()
        return
      case 'Escape':
        selectedId.value = null
        return
      default:
        if (current && options.onDigit?.(event.key, current)) event.preventDefault()
    }
  }

  onMounted(() => window.addEventListener('keydown', onKeydown))
  onUnmounted(() => window.removeEventListener('keydown', onKeydown))

  return { selectedId, pending, select, pendingActionFor, neighborOf, keepPlace, run }
}
