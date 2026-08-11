package com.example.keyboardcommands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局常量（零 MC 依赖，供 common 模块与各版本模块共用）。
 */
public final class Constants {

	public static final String MOD_ID = "command-keybind";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Constants() {
	}
}
