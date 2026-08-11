# 多版本构建方案 C：多子工程聚合（1.16.1 ~ 26.2 稳定版）

> 本文件是"一条命令构建全部 MC 版本"的执行蓝图与分组表。
> 已按调研结果修正：11 个编译基准组，组内小版本共用同一 jar。

## 一、目标

支持 Minecraft 1.16.1 ~ 26.2 的**稳定版**（不含 1.19.0 / 测试版 / 快照），一条 `./gradlew build` 产出全部 jar。

## 二、核心原理：intermediary 兼容

- 混淆版（≤1.21.11）模组针对 Fabric **intermediary 映射**编译，该映射在小版本间保持稳定，只要相关 API 没变，同一份 jar 可跑多个小版本。
- 26.1+ 为官方未混淆命名空间，与混淆版**不可共用产物**。
- 官方不保证 100% 兼容，组内其余小版本需**至少启动实测一次**。

## 三、版本分组表（编译基准 = 该组唯一需编译的版本）

| 组 | 覆盖版本 | 编译基准 | 强制拆组原因 |
|---|---|---|---|
| A | 1.16.1–1.16.5 | **1.16.5** | `player.chat(String)` 发指令；`displayClientMessage(Component,false)`；`render(PoseStack)`；`addButton` |
| B | 1.17–1.18.2 | **1.18.2** | `addRenderableWidget`（1.17+）；JDK≥17 |
| C | 1.19.1–1.19.2 | **1.19.2** | `LocalPlayer.sendCommand(String,Component)` |
| D | 1.19.3–1.19.4 | **1.19.4** | `ClientPacketListener.sendCommand(String)` |
| E | 1.20.0–1.20.1 | **1.20.1** | `render(GuiGraphics)`（GuiGraphics 1.20 引入） |
| F | 1.20.2–1.20.6 | **1.20.6** | `mouseScrolled` 4 参化（1.20.2）；1.20.3/1.20.4 映射哈希相同铁定兼容 |
| G | 1.21.0–1.21.8 | **1.21.8** | 与 F 无断裂，`sendCommand` 混淆名恒为 `c` |
| H | 1.21.9–1.21.10 | **1.21.10** | 输入事件重构（KeyEvent/MouseButtonEvent）；`KeyMapping.Category` |
| I | 1.21.11 | **1.21.11** | `init(int,int)`；新 Screen 构造；最后混淆版 |
| J | 26.1 | **26.1** | 官方未混淆命名 + `extractRenderState`/`GuiGraphicsExtractor` + `net.fabricmc.fabric-loom` |
| K | 26.2 | **26.2** | `Gui.setScreen`、Font 绘制移除、Gui/Hud 重组 |

> 放弃 1.19.0：其 `sendCommand(MessageSigner,String,Component)` 签名独一无二，代价高收益低。

## 四、前提条件

1. 统一 **Gradle 9.5 + JDK 25 跑 Gradle**；各子模块用 **Java Toolchain** 指定编译目标：
   - A → Java 8；B/C/D/E/F/G → Java 17；H/I/J/K → Java 21/25
2. 机器已装 JDK：8（已装）、17（已装）、21（已装）、25（已装）。
3. 插件 ID：
   - 26.x（非混淆）→ `net.fabricmc.fabric-loom`
   - ≤1.21.11（混淆）→ `net.fabricmc.fabric-loom-remap`
   - 混淆子项目依赖用 `modImplementation`，非混淆用 `implementation`。

## 五、目录结构

```
command-keybind/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── common/                          ← 零 MC 依赖（纯 Java）
│   └── src/main/java/com/example/keyboardcommands/
│       ├── api/                     ← Platform、BoundKey 纯接口
│       ├── config/                  ← BindingEntry、ModConfig、ConfigManager
│       └── input/BindingManager.java← 按键逻辑（不 import net.minecraft）
├── mod-26_1/                        ← J 组（现有代码迁移至此）
├── mod-26_2/                        ← K 组
├── mod-1_21_11/                     ← I 组（后续）
└── ...
```

## 六、common 模块边界（零 MC 依赖）

- `api/Platform`、`api/BoundKey`：纯接口，方法签名无 MC 类型。
- `config/`：数据模型 + Gson 读写。
- `input/BindingManager`：按键检测与指令队列逻辑，不 import `net.minecraft`，通过回调/接口获得"是否在游戏内、是否打开界面"。
- **GUI 不下沉 common**：各版本模块内各自实现（Screen API 各段不同）。

## 七、各版本模块职责

每个 `mod-XX` 子工程：
1. `FabricPlatformXX`（实现 `common.api.Platform`）：配置目录、发指令、发消息、开界面、建/注册按键。
2. `ConfigScreen` / `EditCommandsScreen`（该版本控件 API）。
3. `KeyboardCommandsClient`（ClientModInitializer）。
4. `KeyboardCommands`（main 入口，仅常量）。
5. `fabric.mod.json` + `assets/command-keybind/lang/*.json`。

版本差异速查：
| 能力 | A(1.16) | E(1.20) | I(1.21.11) | J(26.1) | K(26.2) |
|---|---|---|---|---|---|
| 发指令 | `player.chat(s)` | `conn.sendCommand(s)` | 同左 | 同左 | 同左 |
| 发消息 | `displayClientMessage` | `sendSystemMessage` | 同左 | 同左 | 同左 |
| GUI 渲染 | `render(PoseStack)` | `render(GuiGraphics)` | 同左 | `extractRenderState(GuiGraphicsExtractor)` | 同左+Font/Gui 重组 |
| Screen 输入 | `keyPressed(int,int,int)` | 同左 | `keyPressed(KeyEvent)` | 同左 | 同左 |
| 按键注册 | fabric-key-mapping-api（对应版本） | 同左 | 同左 | 同左 | 同左 |

## 八、Fabric 依赖版本矩阵（来自 maven.fabricmc.net）

| MC | 最后 Fabric API | Loader | 最小 Java |
|---|---|---|---|
| 1.16.x | `0.42.0+1.16` | 0.19.3 | 8 |
| 1.17.x | `0.46.1+1.17` | 0.19.3 | 16 |
| 1.18.x | `0.77.0+1.18.2` | 0.19.3 | 17 |
| 1.19.x | `0.87.2+1.19.4` | 0.19.3 | 17 |
| 1.20.x | `0.99.4+1.20.6` | 0.19.3 | 17 |
| 1.21.x | `0.141.6+1.21.11` | 0.19.3 | 21 |
| 26.1 | `0.155.2+26.1.2` | 0.19.3 | 25 |
| 26.2 | `0.157.0+26.2` | 0.19.3 | 25 |

## 九、决策记录

- 2026-08-11：确定方案 C，版本范围 1.16.1~26.2 稳定版，放弃 1.19.0。
- 2026-08-11：修正架构——GUI 与 BindingManager 的 MC 依赖不下沉 common。
- 2026-08-11：采纳 11 组编译基准分组（基于逐版本映射表比对）。
- 2026-08-11：执行策略——先落地 common + 26.1/26.2 验证流水线，再扩展其余组。
