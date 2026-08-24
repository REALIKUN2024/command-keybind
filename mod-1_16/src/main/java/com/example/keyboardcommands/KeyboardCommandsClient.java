package com.example.keyboardcommands;

import com.example.keyboardcommands.api.Platform;
import com.example.keyboardcommands.config.ConfigManager;
import com.example.keyboardcommands.input.BindingManager;
import com.example.keyboardcommands.platform.v1_16.FabricPlatform1_16;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class KeyboardCommandsClient implements ClientModInitializer {
	public static final Platform PLATFORM = new FabricPlatform1_16();
	public static final ConfigManager CONFIG_MANAGER = new ConfigManager(PLATFORM);
	public static final BindingManager BINDING_MANAGER = new BindingManager(PLATFORM, CONFIG_MANAGER);

	@Override
	public void onInitializeClient() {
		CONFIG_MANAGER.load();
		BINDING_MANAGER.init();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean inGame = client.screen == null && client.player != null;
			BINDING_MANAGER.onEndTick(inGame);
		});
		Constants.LOGGER.info("[Command Keybind] Client initialized (1.16)");

		// 1.16 的 fabric-resource-loader（fabric-api 0.42.0 内置 0.4.8）在 ClientLanguage 加载之后
		// 才把 mod 资源包注册进资源管理器，导致首次语言加载未收集本模组翻译键（界面显示原始键名、
		// 按钮文字超长溢出）。客户端启动完成后强制语言管理器重载一次以修复。
		ClientLifecycleEvents.CLIENT_STARTED.register(client ->
			client.getLanguageManager().onResourceManagerReload(client.getResourceManager()));
	}
}
