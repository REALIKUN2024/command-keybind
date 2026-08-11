package com.example.keyboardcommands.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 1.19.4 配置界面（PoseStack 时代，手工布局占位实现）。
 *
 * <p>Button 用 builder 构造。</p>
 */
public class ConfigScreen extends Screen {

	private static final Component TITLE = Component.translatable("commandkeybind.screen.title");

	private final Screen lastScreen;

	public ConfigScreen(Screen lastScreen) {
		super(TITLE);
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(centerX - 100, this.height / 2 - 10, 200, 20)
			.build());
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.lastScreen);
	}
}
