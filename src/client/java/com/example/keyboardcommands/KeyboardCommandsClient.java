package com.example.keyboardcommands;

import com.example.keyboardcommands.api.Platform;
import com.example.keyboardcommands.config.ConfigManager;
import com.example.keyboardcommands.input.BindingManager;
import com.example.keyboardcommands.platform.v26_1.FabricPlatform26_1;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class KeyboardCommandsClient implements ClientModInitializer {
	public static final Platform PLATFORM = new FabricPlatform26_1();
	public static final ConfigManager CONFIG_MANAGER = new ConfigManager(PLATFORM);
	public static final BindingManager BINDING_MANAGER = new BindingManager(PLATFORM, CONFIG_MANAGER);

	@Override
	public void onInitializeClient() {
		CONFIG_MANAGER.load();
		BINDING_MANAGER.init();
		ClientTickEvents.END_CLIENT_TICK.register(client -> BINDING_MANAGER.onEndTick());
		KeyboardCommands.LOGGER.info("[Command Keybind] Client initialized");
	}
}