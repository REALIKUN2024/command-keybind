package com.example.keyboardcommands.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * 1.16 配置界面（PoseStack 时代，手工布局占位实现）。
 *
 * <p>说明：1.16 无 HeaderAndFooterLayout / StringWidget，Button 用构造式。
 * 此实现先保证可编译可打开，完整列表/编辑功能在后续完善。</p>
 */
public class ConfigScreen extends Screen {

	private static final Component TITLE = new TranslatableComponent("commandkeybind.screen.title");

	private final Screen lastScreen;

	public ConfigScreen(Screen lastScreen) {
		super(TITLE);
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		this.addButton(new Button(centerX - 100, this.height / 2 - 10, 200, 20,
			CommonComponents.GUI_DONE, button -> this.onClose()));
	}

	@Override
	public void onClose() {
		// TODO: 保存配置
		this.minecraft.setScreen(this.lastScreen);
	}
}
