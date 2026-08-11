# 实施计划：Command Keybind（指令快捷键）Fabric 26.1 模组

## 一、项目概述

开发一个**仅客户端**的 Fabric 26.1 模组：玩家可通过按下指定按键快速执行预先配置的指令，并提供游戏内配置界面，自由增减"按键-指令"绑定。

## 二、已确认的需求（用户确认）

1. 仅客户端模组，不涉及服务端逻辑。
2. 配置文件存放于 `config` 目录。
3. 无冷却机制；触发后在聊天栏向玩家显示"已执行 xxx"；不做变量替换。
4. ModMenu 支持，集成用 ModMenu jar 位于 `D:\Downloads\modmenu-18.0.0.jar`。
5. 提供可修改的"打开配置菜单"按键，并注册进原版游戏的按键绑定设置（Controls 界面）。

## 三、多版本接口预留架构

为避免版本 API 与业务逻辑耦合，核心逻辑通过**平台抽象层（Platform Interface）**调用版本相关 API，便于将来移植到其他 MC 版本时仅替换实现类。

```
src/client/java/com/example/keyboardcommands/
├── KeyboardCommandsClient.java      # ClientModInitializer 入口（注册按键、事件）
├── KeyboardCommands.java            # main 入口（仅持有 ModId/常量）
├── api/                              # ── 平台抽象层（多版本接口预留）──
│   ├── Platform.java                #   抽象接口：配置目录、指令发送、消息显示、屏幕打开、按键注册
│   └── VersionProvider.java         #   提供当前版本平台实现的工厂
├── platform/
│   └── v26_1/
│       └── FabricPlatform26_1.java  #   26.1 版实现（唯一接触 MC 内部 API 的地方）
├── config/
│   ├── BindingEntry.java            # 单个绑定数据模型（名称、按键、指令列表）
│   ├── ModConfig.java               # 配置数据模型（绑定列表 + 打开菜单按键）
│   ├── ConfigManager.java           # config 目录 JSON 读写
│   └── ConfigIO.java                # Gson 序列化 / 反序列化
├── input/
│   └── BindingManager.java          # 从配置生成 KeyMapping、监听 tick、执行指令
└── gui/
    ├── ConfigScreen.java            # 主配置界面
    └── EditCommandsScreen.java      # 指令编辑界面
```

**抽象层职责分配**（均以 26.1 实现为准，接口独立于版本）：

| 抽象接口方法 | 26.1 实现方式 |
|---|---|
| `Path getConfigDir()` | `FabricLoader.getInstance().getConfigDir()` |
| `void sendCommand(String)` | `player.connection.sendCommand(cmd)`（去 `/` 前缀） |
| `void sendSystemMessage(Component)` | `player.sendSystemMessage(Component)` |
| `void setScreen(Screen)` | `Minecraft.getInstance().setScreen(screen)` |
| `KeyMapping createKeyMapping(name, keysym, category)` | `new KeyMapping(name, keysym, category)` |
| `void registerKeyMapping(KeyMapping)` | `KeyMappingHelper.registerKeyMapping(...)` |

## 四、配置模型与文件格式

配置文件：`config/command-keybind.json`

```json
{
  "openMenuKey": "key.keyboard.k",            // 打开配置菜单按键（InputConstants.Key.getName() 存储）
  "bindings": [
    {
      "name": "创造模式",
      "key": "key.keyboard.g",                // 触发按键
      "commands": ["/gamemode creative", "/say 已进入创造模式"]
    },
    {
      "name": "传送回家",
      "key": "key.keyboard.h",
      "commands": ["/tp ~ 256 ~"]
    }
  ]
}
```

- 按键以 `InputConstants.Key.getName()` 字符串持久化（原版 `saveString()` 同款格式），反序列化用 `InputConstants.getKey(name)`。
- 指令保存时**含 `/` 前缀**，执行时由平台层剥离 `/` 后调用 `sendCommand`。

## 五、功能实现方案（基于已查证的原版接口）

### 5.1 按键注册（原版 + Fabric KeyMapping API）
- 打开菜单按键：静态注册。在 `onInitializeClient()` 中创建 `KeyMapping("key.keyboardcommands.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC)`，经 `KeyMappingHelper.registerKeyMapping()` 注册 → **自动出现在原版 Controls 按键绑定界面**（Fabric 通过 mixin 注入 Options，已确认 impl 源码）。
- 每个绑定条目：动态创建 `KeyMapping`。配置加载/修改后重建。
  - 注意：`KeyMappingHelper.registerKeyMapping` 仅限 Options 初始化前调用，**绑定条目的 KeyMapping 不能走该路径**；改用"监听 tick 中直接轮询 `consumeClick()`"方式（见 5.2），不与原版 Controls 界面耦合。

### 5.2 按键监听与执行（原版 KeyMapping + Fabric 事件）
- `ClientTickEvents.END_CLIENT_TICK.register(...)` 中遍历所有绑定 KeyMapping，调用 `keyMapping.consumeClick()`。
- 命中后：对每条指令调用平台层 `sendCommand()`（剥离 `/`），随后调用 `sendSystemMessage(Component.translatable("keyboardcommands.chat.executed", command))` 显示"已执行 xxx"。
- 打开菜单按键命中 → `setScreen(new ConfigScreen(...))`。

### 5.3 配置界面（全部使用原版控件）
主界面 `ConfigScreen extends Screen`，采用原版 `OptionsSubScreen` 同款 `layout`（HeaderAndFooterLayout）结构：
- 中间内容区：`ContainerObjectSelectionList` 列表（复用原版 KeyBindsList 的 Entry 模式），每行 = 绑定名称 + [改键按钮] + [编辑指令按钮] + [删除按钮]。
- 底部 footer：`LinearLayout` 水平排列 [添加绑定] [完成] 按钮（`CommonComponents.GUI_DONE`）。
- 改键交互：复用原版 KeyBindsScreen 的 `selectedKey` 捕获模式（`keyPressed`/`mouseClicked` 捕获，Esc 取消）。

指令编辑界面 `EditCommandsScreen extends Screen`：
- 内容区：`ContainerObjectSelectionList`，每行 = 指令 `EditBox`（原版控件）+ [删除] 按钮。
- footer：[添加指令] [完成]。

### 5.4 ModMenu 集成
- 实现 `com.terraformersmc.modmenu.api.ModMenuApi`，`getModConfigScreenFactory()` 返回 `parent -> new ConfigScreen(parent)`。
- `fabric.mod.json` 增加 `modmenu` entrypoint。
- `build.gradle` 以 `compileOnly` 方式依赖 `D:\Downloads\modmenu-18.0.0.jar`（可选依赖，modmenu 不在时不影响主功能）。

## 六、构建配置调整

| 项 | 现状 | 改为 |
|---|---|---|
| settings.gradle 项目名 | `template-mod` | `command-keybind` |
| mod id | `template-mod` | `command-keybind` |
| 包名 | `com.example` | `com.example.keyboardcommands` |
| 版本 | `1.0.0` | 初始 1.0.0 |
| fabric.mod.json | 默认模板 | 重写（id/name/entrypoint/依赖），`environment: "client"`，`depends` 增加 `modmenu` 于 `suggests` |
| build.gradle | — | 增加 ModMenu `compileOnly` 依赖；清理示例 mixin |

## 七、开发步骤（顺序）

1. **工程重命名与配置**：settings.gradle、gradle.properties、fabric.mod.json、包名整理，删除模板示例代码与 mixin。
2. **平台抽象层**：`api/Platform.java`、`VersionProvider` 及 26.1 实现。
3. **配置模块**：数据模型 + ConfigManager（Gson 读写、容错、默认配置生成）。
4. **按键与执行模块**：BindingManager（KeyMapping 创建/重建、tick 监听、指令执行、消息提示）。
5. **UI 设计确认**（独立 md，见 `docs/UI_DESIGN.md`，需用户确认后方可编写 UI 代码）。
6. **配置界面**：ConfigScreen + EditCommandsScreen（原版控件）。
7. **ModMenu 集成**：ModMenuApi 实现 + entrypoint。
8. **语言文件**：`assets/command-keybind/lang/en_us.json`、`zh_cn.json`。
9. **构建验证**：`gradlew build`，检查产物与 log。

## 八、版本号规则（遵循 AGENTS.md）
- 本次为新增功能开发，初始版本定为 **1.0.0**。
- 后续：修复 bug 不升版本；新增功能 +0.1。

## 九、待用户确认的开放问题
1. 初始版本号是否用 1.0.0？（默认采纳）
2. 打开配置菜单的默认按键建议 `K`（GLFW_KEY_K），可接受？若冲突会在原版 Controls 中改。
3. 绑定条目是否需要"启用/禁用"开关（Checkbox）？还是简化不设？（当前计划未包含）
4. 同一绑定内多条指令是**顺序执行**（每次按键依次全部发送），确认无误？
