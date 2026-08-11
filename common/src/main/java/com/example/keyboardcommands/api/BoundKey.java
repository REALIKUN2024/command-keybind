package com.example.keyboardcommands.api;

/**
 * 平台抽象层：按键绑定句柄。
 *
 * <p>本接口不依赖任何 Minecraft 具体类，屏蔽版本差异。</p>
 *
 * <p>按键的持久化使用 {@link InputConstants.Key#getName()} 字符串格式
 * （如 "key.keyboard.g"），这是 Minecraft 各版本通用的按键编码。</p>
 */
public interface BoundKey {
	/** 按键绑定的唯一名称（翻译键）。 */
	String getName();

	/** 消耗一次按下（返回 true 表示当前帧发生了按下）。 */
	boolean consumeClick();

	/** 是否未绑定任何按键。 */
	boolean isUnbound();

	/** 从持久化字符串恢复绑定（如 "key.keyboard.g"）。 */
	void setKey(String saveName);

	/** 解除绑定。 */
	void setUnbound();

	/** 当前绑定按键的持久化字符串（保存配置用）。 */
	String getKeyName();

	/** 按键的显示文本（界面用，已本地化）。 */
	String getDisplayName();
}
