package com.example.keyboardcommands.api;

import java.nio.file.Path;

/**
 * 平台抽象层：版本相关能力接口。
 *
 * <p>这是全工程唯一需要面对版本差异的业务入口。
 * 业务逻辑（配置、按键监听、指令执行、UI）只依赖本接口，
 * 具体实现位于 {@code platform/v26_1/FabricPlatform26_1}，
 * 将来移植其他版本时仅需新增对应实现类。</p>
 *
 * <p>接口方法签名仅使用 JDK 类型，不依赖任何 Minecraft 类。</p>
 */
public interface Platform {

	/**
	 * 返回配置文件所在目录（Minecraft 的 config 目录）。
	 */
	Path getConfigDir();

	/**
	 * 创建并注册一个按键绑定。
	 *
	 * @param name            按键唯一名称（翻译键），如 {@code key.keyboardcommands.binding.0}
	 * @param defaultKeyName  默认绑定的按键编码，如 {@code key.keyboard.g}；可为空表示未绑定
	 * @param showInControls  true 时该按键会出现在原版 Controls 按键绑定界面
	 *                        （仅限 Options 初始化前创建的静态按键）；
	 *                        false 表示仅用于本模组内部监听，不进入原版界面
	 * @return 按键句柄
	 */
	BoundKey createKey(String name, String defaultKeyName, boolean showInControls);

	/**
	 * 以玩家身份发送一条指令。
	 *
	 * @param command 指令内容，可含或不含前导 '/'，实现会统一处理
	 */
	void sendCommand(String command);

	/**
	 * 在聊天栏向玩家显示一条本地化系统消息。
	 *
	 * @param translationKey 语言文件翻译键
	 * @param args           翻译参数
	 */
	void sendSystemMessage(String translationKey, Object... args);

	/**
	 * 打开本模组的配置界面。
	 */
	void openConfigScreen();
}
