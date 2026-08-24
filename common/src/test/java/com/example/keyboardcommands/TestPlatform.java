package com.example.keyboardcommands;

import com.example.keyboardcommands.api.BoundKey;
import com.example.keyboardcommands.api.Platform;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试用 Platform：记录指令/消息发送与界面打开次数；
 * 按键由 {@link FakeBoundKey} 模拟，测试代码可主动触发按下。
 */
public final class TestPlatform implements Platform {

	public final List<String> sentCommands = new ArrayList<>();
	public final List<String> sentMessages = new ArrayList<>();
	public int openScreenCount;

	private final Path configDir;
	private final List<FakeBoundKey> createdKeys = new ArrayList<>();

	public TestPlatform(Path configDir) {
		this.configDir = configDir;
	}

	@Override
	public Path getConfigDir() {
		return this.configDir;
	}

	@Override
	public BoundKey createKey(String name, String defaultKeyName, boolean showInControls) {
		FakeBoundKey key = new FakeBoundKey(name, defaultKeyName);
		this.createdKeys.add(key);
		return key;
	}

	@Override
	public void sendCommand(String command) {
		this.sentCommands.add(command);
	}

	@Override
	public void sendSystemMessage(String translationKey, Object... args) {
		this.sentMessages.add(translationKey);
	}

	@Override
	public void openConfigScreen() {
		this.openScreenCount++;
	}

	/** 按键名 -> 实例（openMenu 为 "key.commandkeybind.open_menu"，绑定 i 为 "key.commandkeybind.binding.i"）。 */
	public FakeBoundKey getKey(String name) {
		for (FakeBoundKey key : this.createdKeys) {
			if (key.getName().equals(name)) {
				return key;
			}
		}
		throw new IllegalArgumentException("No key created with name " + name);
	}

	/** 已创建按键实例总数（openMenu + 绑定）。 */
	public int createdKeyCount() {
		return this.createdKeys.size();
	}

	/** 模拟原版 KeyMapping：未绑定时 consumeClick 恒为 false。 */
	public static final class FakeBoundKey implements BoundKey {
		private final String name;
		private String keyName;
		private boolean pressed;

		private FakeBoundKey(String name, String keyName) {
			this.name = name;
			this.keyName = keyName == null ? "" : keyName;
		}

		/** 模拟一次按下。 */
		public void press() {
			this.pressed = true;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean consumeClick() {
			if (this.isUnbound()) {
				return false;
			}
			boolean p = this.pressed;
			this.pressed = false;
			return p;
		}

		@Override
		public boolean isUnbound() {
			return this.keyName == null || this.keyName.isEmpty();
		}

		@Override
		public void setKey(String saveName) {
			this.keyName = saveName == null ? "" : saveName;
		}

		@Override
		public void setUnbound() {
			this.keyName = "";
		}

		@Override
		public String getKeyName() {
			return this.keyName;
		}

		@Override
		public String getDisplayName() {
			return this.keyName;
		}
	}
}
