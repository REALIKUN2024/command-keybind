package com.example.keyboardcommands.platform.v1_18;

import com.example.keyboardcommands.Constants;
import com.example.keyboardcommands.api.BoundKey;
import com.example.keyboardcommands.api.Platform;
import com.example.keyboardcommands.gui.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * Minecraft 1.18.2 平台实现（官方映射）。
 *
 * <p>差异：发指令用 {@code LocalPlayer.chat}，发消息用
 * {@code displayClientMessage}，按键注册走 keybinding.v1 包，
 * KeyMapping 分类为字符串。</p>
 */
public final class FabricPlatform1_18 implements Platform {

	/** 本模组专属按键分类（字符串形式，显示名用翻译键 key.category.command-keybind.command-keybind，与 1.21.10+ 的 KeyMapping.Category 同键）。 */
	public static final String CATEGORY = "key.category.command-keybind.command-keybind";

	private static Minecraft minecraft() {
		return Minecraft.getInstance();
	}

	@Override
	public Path getConfigDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public BoundKey createKey(String name, String defaultKeyName, boolean showInControls) {
		InputConstants.Key defaultKey = parseKey(defaultKeyName);
		KeyMapping keyMapping = new KeyMapping(name, defaultKey.getType(), defaultKey.getValue(), CATEGORY);
		if (showInControls) {
			KeyBindingHelper.registerKeyBinding(keyMapping);
		}
		return new FabricBoundKey(keyMapping);
	}

	private static InputConstants.Key parseKey(String name) {
		if (name == null || name.isEmpty()) {
			return InputConstants.UNKNOWN;
		}
		try {
			return InputConstants.getKey(name);
		} catch (IllegalArgumentException e) {
			return InputConstants.UNKNOWN;
		}
	}

	@Override
	public void sendCommand(String command) {
		if (minecraft().player == null) {
			return;
		}
		String cmd = command.trim();
		if (cmd.isEmpty()) {
			return;
		}
		// 1.18 无 ClientPacketListener.sendCommand：执行指令需 chat("/cmd")，
		// 由服务端按消息首字符 '/' 解析为指令。剥离前缀会导致当作聊天消息发送。
		if (!cmd.startsWith("/")) {
			cmd = "/" + cmd;
		}
		minecraft().player.chat(cmd);
	}

	@Override
	public void sendSystemMessage(String translationKey, Object... args) {
		if (minecraft().player == null) {
			return;
		}
		minecraft().player.displayClientMessage(new TranslatableComponent(translationKey, args), false);
	}

	@Override
	public void openConfigScreen() {
		minecraft().setScreen(new ConfigScreen(minecraft().screen));
	}

	/** {@link BoundKey} 的 1.18 实现，包装原版 {@link KeyMapping}。 */
	private static final class FabricBoundKey implements BoundKey {
		private final KeyMapping keyMapping;

		private FabricBoundKey(KeyMapping keyMapping) {
			this.keyMapping = keyMapping;
		}

		@Override
		public String getName() {
			return this.keyMapping.getName();
		}

		@Override
		public boolean consumeClick() {
			return this.keyMapping.consumeClick();
		}

		@Override
		public boolean isUnbound() {
			return this.keyMapping.isUnbound();
		}

		@Override
		public void setKey(String saveName) {
			this.keyMapping.setKey(parseKey(saveName));
			// setKey 只改 this.key、不更新静态 MAP（按键事件按 MAP 查找），须 resetMapping 重建 MAP
			KeyMapping.resetMapping();
		}

		@Override
		public void setUnbound() {
			this.keyMapping.setKey(InputConstants.UNKNOWN);
		}

		@Override
		public String getKeyName() {
			return this.keyMapping.saveString();
		}

		@Override
		public String getDisplayName() {
			return this.keyMapping.getTranslatedKeyMessage().getString();
		}
	}
}
