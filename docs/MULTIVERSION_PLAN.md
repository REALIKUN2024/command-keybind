# 多版本构建方案 C：多子工程聚合

> 本文档记录"一条命令构建所有 MC 版本"的最终架构方案，供将来多版本支持时直接参考执行。
> 当前阶段仍使用**单工程**（方案 A），本文件仅作为升级蓝图，暂不实施。

## 一、目标

在将来需要同时支持多个 Minecraft 版本时，用 **一条 `./gradlew build` 命令产出所有版本的 mod jar**。

## 二、前提条件

1. 机器上安装所需**全部 JDK 版本**：
   - MC 26.1 → JDK 25（已装：`C:\Program Files\Java\jdk-25.0.3`）
   - MC 1.21.x → JDK 21（已装：`C:\Program Files\Java\jdk-21.0.10`）
   - 其他版本按需。
2. 每个子模块在 `build.gradle` 中声明 **Gradle Toolchain**，让 Gradle 自动选择对应 JDK：
   ```gradle
   java {
       toolchain {
           languageVersion = JavaLanguageVersion.of(25)  // 各子模块不同
       }
   }
   ```
3. 子模块之间必须**构建隔离**：不要用 `includeBuild` 互相引用，避免 JDK 冲突；共享代码走 `common` 模块。

## 三、目标目录结构

```
command-keybind/
├── settings.gradle             ← 根聚合：include 各子模块
├── build.gradle                ← 根聚合配置（subprojects 统一配置）
├── gradle.properties           ← 公共属性（org.gradle.jvmargs 等）
├── common/                     ← 共享业务逻辑（零 MC 依赖，唯一共享代码）
│   └── src/main/java/com/example/keyboardcommands/
│       ├── api/                ← 平台抽象层 Platform.java、VersionProvider.java
│       ├── config/             ← 数据模型 + ConfigManager
│       ├── input/              ← BindingManager（按键监听、指令执行）
│       └── gui/                ← ConfigScreen、EditCommandsScreen
├── mod-26_1/                   ← 26.1 版实现
│   ├── build.gradle            ← toolchain=25 + 依赖 common
│   └── src/client/java/com/example/keyboardcommands/platform/v26_1/
│       └── FabricPlatform26_1.java
│       └── KeyboardCommandsClient.java      ← ClientModInitializer 入口
│       └── KeyboardCommands.java            ← main 入口
│   └── src/main/resources/
│       └── fabric.mod.json
├── mod-1_21_4/                 ← 将来新增的 1.21.4 实现（示例）
│   ├── build.gradle            ← toolchain=21 + 依赖 common
│   └── src/client/java/.../platform/v1_21_4/FabricPlatform1_21_4.java
│   └── src/main/resources/fabric.mod.json
└── docs/
    ├── IMPLEMENTATION_PLAN.md
    └── UI_DESIGN.md
```

## 四、关键配置要点

### 4.1 settings.gradle（根）
```gradle
pluginManagement {
    repositories {
        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = 'command-keybind'
include 'common'
include 'mod-26_1'
// 将来：include 'mod-1_21_4'
```

### 4.2 common/build.gradle
```gradle
plugins { id 'java' }
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
// 不声明 MC / Fabric 依赖 —— common 必须零 MC 依赖
```

### 4.3 mod-26_1/build.gradle
```gradle
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}
repositories { /* Fabric maven 等 */ }
dependencies {
    implementation project(':common')
    implementation "net.fabricmc:fabric-loader:${loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version_26_1}"
    compileOnly files("D:/Downloads/modmenu-18.0.0.jar")
}
loom {
    splitEnvironmentSourceSets()
    mods { "command-keybind" { sourceSet sourceSets.main; sourceSet sourceSets.client } }
}
processResources {
    // 依赖 fabric.mod.json 中 ${version} 展开
}
```

### 4.4 mod-1_21_4/build.gradle（将来）
同上，但：
- `toolchain = JavaLanguageVersion.of(21)`
- `fabric_api_version_1_21_4` 用对应版本
- 依赖 `modmenu` 对应旧版 jar
- fabric.mod.json 中 `"environment": "client"`、版本号按各自规则

## 五、从当前单工程升级到 C 的迁移步骤

1. 在 `gradle.properties` 中拆分各版本独立属性（如 `fabric_api_version_26_1`、`fabric_api_version_1_21_4`）。
2. 新建 `common/` 目录，把现有 `src/client/java/com/example/keyboardcommands/` 下的 **api / config / input / gui** 四个包原样移动过去。
3. 新建 `mod-26_1/`，把客户端入口类（`KeyboardCommandsClient`、`KeyboardCommands`）与 `platform/v26_1/` 实现、`fabric.mod.json`、资源文件移过去。
4. 根 `settings.gradle` 改为 `include` 结构。
5. 根 `build.gradle` 改为聚合配置（`subprojects` 统一属性）。
6. `mod-26_1` 的 `dependencies` 改为 `implementation project(':common')`。
7. 构建验证：`./gradlew build`（会依次编译 common → 各子模块）。
8. **业务代码零改动**——`common` 内不出现任何 `net.minecraft` / `net.fabricmc` 类引用。

## 六、注意事项

- **共享代码纯净性**：`common` 内严禁 import 任何 `net.minecraft.*` 或 `net.fabricmc.*` 类，这是升级成败的关键。所有版本相关调用必须走 `api/Platform` 接口。
- **GUI 控件的版本差异**：`Screen`、`Button`、`EditBox` 等控件在不同版本 API 有差异（26.1 用 `GuiGraphicsExtractor`、旧版用 `GuiGraphics`）。若 UI 也要跨版本共享，`gui` 包需要做第二层适配（`ui/` 子接口）；否则 UI 也可下沉到各子模块实现。**当前设计：UI 留在 common，若移植时控件 API 差异过大，再将 UI 迁到各版本模块。**
- **JDK 切换**：即使配置了 toolchain，首次构建某版本时 Gradle 会下载/查找对应 JDK（需已在系统安装并配置 `org.gradle.java.installations.paths` 或自动探测）。
- **ModMenu 依赖**：各子模块需要对应 MC 版本的 ModMenu jar，版本不同不能共用。

## 七、决策记录

- 2026-08-11：确定采用方案 C 蓝图，当前仍按方案 A 单工程开发，避免过早引入多 JDK 复杂度；待第二个版本需求出现时按本文件执行升级。
