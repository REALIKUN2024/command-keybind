package com.example.keyboardcommands.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * 1.18 配置界面（PoseStack 时代，手工布局占位实现）。
 *
 * <p>与 1.16 的区别：使用 addRenderableWidget 而非 addButton。</p>
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
		this.addRenderableWidget(new Button(centerX - 100, this.height / 2 - 10, 200, 20,
			CommonComponents.GUI_DONE, button -> this.onClose()));
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.lastScreen);
	}
}
