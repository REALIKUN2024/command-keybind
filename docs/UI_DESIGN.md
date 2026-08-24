# UI 设计文档：指令快捷键 配置界面

> 本文件描述配置界面的完整布局与交互设计，全部元素仅使用 Minecraft 原版控件。
> **需用户确认后方可编写 UI 代码。**

## 一、控件来源声明

以下控件全部为 Minecraft 原版已存在控件/类，无任何自绘自定义元素：

| 用途 | 原版控件 |
|---|---|
| 界面基类 | `net.minecraft.client.gui.screens.Screen` |
| 布局容器 | `net.minecraft.client.gui.layouts.HeaderAndFooterLayout`、`LinearLayout` |
| 主列表 | `net.minecraft.client.gui.components.ContainerObjectSelectionList` |
| 普通按钮 | `net.minecraft.client.gui.components.Button` |
| 文本输入 | `net.minecraft.client.gui.components.EditBox` |
| 完成按钮文案 | `net.minecraft.network.chat.CommonComponents.GUI_DONE` |
| 标题 | `Component.translatable(...)`（Screen 自带标题渲染） |

## 二、主界面 ConfigScreen（配置总览）

整体复用原版 `OptionsSubScreen` 的 layout 结构：**标题（顶部）+ 内容列表（中间）+ 按钮栏（底部）**。

```
┌────────────────────────────────────────────┐
│        指令快捷键 配置                │  ← 标题（原版 Screen 渲染）
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ [A] 创造模式    [改键: G] [编辑] [×]   │ │  ← 列表行（ContainerObjectSelectionList）
│ │ [B] 传送回家    [改键: H] [编辑] [×]   │ │
│ │ [C] 全屏截图    [改键: F7][编辑] [×]   │ │
│ │   （无条目时显示提示文本"暂无绑定"）    │ │
│ └────────────────────────────────────────┘ │
│         [＋ 添加绑定]      [完成]          │  ← 底部按钮栏（LinearLayout 水平）
└────────────────────────────────────────────┘
```

### 每行布局（Entry）
- **左侧**：绑定名称（原版 `StringWidget`/纯文本，非按钮）。
- **中部**：`[改键: X]` 按钮——显示当前按键名称；点击进入"按键捕获"模式（按钮文案变为 `> ... <`，按下任意键绑定、Esc 取消）。**复用原版 KeyBindsScreen 的 selectedKey 交互逻辑**。
- **右侧**：`[编辑]` 按钮 → 打开指令编辑界面；`[×]` 删除按钮 → 删除该行（需二次确认按钮或直接删除，见开放问题 3）。

### 交互细节
- 点击 `[改键]` 后再次点击/按键：捕获新键并写入绑定。
- 点击 `[编辑]`：进入 EditCommandsScreen（见下）。
- 点击 `[×]`：移除该绑定。
- `[＋ 添加绑定]`：在列表末尾新增一行空绑定（名称"新绑定"、默认键未绑定）。
- `[完成]`：保存配置到 `config/command-keybind.json` 并关闭界面，返回原屏幕。

## 三、指令编辑界面 EditCommandsScreen

```
┌────────────────────────────────────────────┐
│        编辑指令 - 创造模式                    │  ← 标题
│   [绑定名称: 创造模式        ]              │  ← 名称 EditBox（可重命名，原版控件）
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ [ /gamemode creative        ] [×]     │ │  ← 行 = EditBox（指令输入）+ 删除
│ │ [ /say 已进入创造模式        ] [×]     │ │
│ └────────────────────────────────────────┘ │
│        [＋ 添加指令]      [完成]          │
└────────────────────────────────────────────┘
```

- **顶部新增名称 EditBox**：可重命名该绑定条目（原版 `EditBox`，放置于 header 区域的垂直 LinearLayout 中，参考原版 `CreateBuffetWorldScreen`）。
- 每行：指令 `EditBox`（可编辑文本，保存时采用其当前值）+ `[×]` 删除。
- `[＋ 添加指令]`：新增一行空 EditBox。
- `[完成]`：回写指令列表与名称到绑定，返回主界面。

## 四、进入方式
1. 原版 Controls 中的"打开配置菜单"按键（默认 K）。
2. ModMenu 模组列表中该模组的"配置"按钮。

## 五、提示文案（语言文件键）
- `keyboardcommands.screen.title`：主界面标题
- `keyboardcommands.screen.edit.title`：指令编辑标题
- `keyboardcommands.screen.add_binding`：添加绑定
- `keyboardcommands.screen.add_command`：添加指令
- `keyboardcommands.screen.edit`：编辑
- `keyboardcommands.screen.delete`：删除
- `keyboardcommands.screen.empty`：暂无绑定
- `keyboardcommands.key.open_menu`：打开配置菜单
- `keyboardcommands.chat.executed`：已执行 %s

## 六、决策记录（2026-08-11 用户确认）

| 问题 | 决定 |
|---|---|
| 主界面是否包含"打开配置菜单按键"的可改键入口？ | 不含，直接在原版 Controls 中改 |
| 删除绑定/指令是否需要二次确认？ | 直接删除，不加确认 |
| 列表滚动 | ContainerObjectSelectionList 自带，默认启用 |
| 绑定行是否需要"启用/禁用" Checkbox？ | 不加，保持简单 |
| 改键捕获状态下按 Esc | **解绑**（清空该绑定键位，非"取消捕获"）——按用户确认实现 |
| 鼠标按键捕获 | 全部版本支持：1.21.10+ 与 26.x 原有；1.16~1.21.8 于 2026-08-11 补齐 |

> 注：捕获模式下 Esc=解绑 与早期设计文档"Esc 取消"措辞不同，以本决策记录为准。
