package com.example.keyboardcommands.config;

import com.example.keyboardcommands.Constants;
import com.example.keyboardcommands.api.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置文件读写管理。
 *
 * <p>配置文件位于 {@code config/command-keybind.json}。</p>
 */
public final class ConfigManager {

	private static final String FILE_NAME = "command-keybind.json";

	private final Path configPath;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private ModConfig config;

	public ConfigManager(Platform platform) {
		this.configPath = platform.getConfigDir().resolve(FILE_NAME);
	}

	/** 从磁盘加载配置；文件不存在时生成默认配置并保存。 */
	public void load() {
		if (Files.exists(this.configPath)) {
			try {
				String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
				ModConfig parsed = this.gson.fromJson(json, ModConfig.class);
				this.config = parsed != null ? parsed : new ModConfig();
				this.config.setBindings(this.config.getBindings());
				this.config.setOpenMenuKey(this.config.getOpenMenuKey());
				return;
			} catch (IOException | RuntimeException e) {
				Constants.LOGGER.error("Failed to load config {}, falling back to default", this.configPath, e);
			}
		}
		this.config = new ModConfig();
		this.save();
	}

	/** 保存当前配置到磁盘。 */
	public void save() {
		try {
			Files.createDirectories(this.configPath.getParent());
			String json = this.gson.toJson(this.config);
			Files.writeString(this.configPath, json, StandardCharsets.UTF_8);
		} catch (IOException e) {
			Constants.LOGGER.error("Failed to save config to {}", this.configPath, e);
		}
	}

	/** 当前内存中的配置（编辑中）。 */
	public ModConfig getConfig() {
		return this.config;
	}

	/** 用新配置替换内存中的配置。 */
	public void setConfig(ModConfig config) {
		this.config = config;
	}
}
