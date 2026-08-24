# Command Keybind 指令快捷键

给按键绑上命令，按一下就执行。纯客户端模组，按设定好的快捷键就能快速发送配置好的指令，不用打开聊天框手打。

## 功能

- 一个绑定可挂多条指令，按一次键全部发出
- 指令之间可设 tick 间隔，适合需要按顺序执行的组合
- 游戏内配置界面，加绑定、删绑定、改名称、改按键都在里面完成
- 配置存 `config/command-keybind.json`，手改也行
- "打开配置菜单"按键默认 K，注册进原版按键设置，归在独立分类下
- 装了 ModMenu 的话，模组列表里会有配置入口（全部版本均支持）

## 支持版本

支持以下 Minecraft 稳定版（只做稳定版，不碰测试版）：

1.16 ~ 26.2

每个版本都是独立子工程，构建产物互不干扰。

## 安装

把对应版本的 jar 放进 `mods` 文件夹，需要 Fabric Loader 和 Fabric API。用哪个版本就装哪个 jar。

## 构建

装好各版本对应的 JDK（1.16 用 Java 8，26.x 用 Java 21+，具体见各子工程 `build.gradle` 的 toolchain；缺失的 toolchain 可由 Gradle 自动下载），然后：

```
gradlew build
```

一条命令，全部版本的 jar 一起出，产物在各自模块的 `build/libs/` 下。

首次构建会自动从 Modrinth CDN 下载各版本对应的 ModMenu jar 到 `libs/modmenu/`（仅编译期引用，不打包、不影响无 ModMenu 运行），需要联网；之后有本地缓存。

common 模块含 JUnit 单元测试（配置读写/容错、按键与指令队列逻辑），构建时自动运行：

```
gradlew :common:test
```

## 配置文件示例

```json
{
  "openMenuKey": "key.keyboard.k",
  "bindings": [
    {
      "name": "创造模式",
      "key": "key.keyboard.g",
      "commands": ["/gamemode creative"],
      "intervalTicks": 0
    }
  ]
}
```

- `intervalTicks`：每条指令间的间隔，单位 tick，0 表示立即连续执行
- 按键用 `key.keyboard.xxx` 这样的原版键名

## 目录结构

```
common/        平台抽象层 + 配置模型 + 按键逻辑（与具体版本无关）
mod-1_16/      1.16 版本实现
mod-26_2/      26.2 版本实现
```

新版本支持只需新增一个子工程，填上该版本的平台实现即可。

## License

CC0-1.0。

---

# Command Keybind (English)

Bind commands to keys. Press a key, the configured commands are sent. A client-side mod that fires pre-set commands from hotkeys, no need to open the chat box.

## Features

- One binding can hold multiple commands, all sent on a single key press
- Optional tick interval between commands, useful for ordered sequences
- In-game config screen to add, remove and rename bindings and edit keys
- Config stored at `config/command-keybind.json`, editable by hand as well
- "Open config menu" key defaults to K, registered in vanilla key binds under its own category
- With ModMenu installed, a config button shows up in the mod list

## Supported versions

Stable Minecraft releases only (no test/experimental builds):

1.16 ~ 26.2

Each version is a separate subproject, outputs do not interfere with each other.

## Install

Drop the jar matching your version into the `mods` folder. Fabric Loader and Fabric API required.

## Build

Install the JDKs for each version (Java 8 for 1.16, Java 21+ for 26.x, see the toolchain block in each subproject's `build.gradle`), then:

```
gradlew build
```

One command builds every version at once. Jars land in each module's `build/libs/`.

## Config example

```json
{
  "openMenuKey": "key.keyboard.k",
  "bindings": [
    {
      "name": "Creative",
      "key": "key.keyboard.g",
      "commands": ["/gamemode creative"],
      "intervalTicks": 0
    }
  ]
}
```

- `intervalTicks`: delay between commands in ticks, 0 fires them all back to back
- Keys use vanilla names like `key.keyboard.xxx`

## Project layout

```
common/        platform abstraction + config model + binding logic (version-agnostic)
mod-1_16/      1.16 implementation
mod-26_2/      26.2 implementation
```

Adding a version means adding a subproject and filling in that version's platform implementation.

## License

CC0-1.0.
