package com.example.keyboardcommands;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyboardCommands implements ModInitializer {
	public static final String MOD_ID = "command-keybind";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 纯客户端模组，无服务端初始化逻辑。
	}
}
