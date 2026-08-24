package com.example.keyboardcommands.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.keyboardcommands.TestPlatform;
import com.example.keyboardcommands.config.BindingEntry;
import com.example.keyboardcommands.config.ConfigManager;
import com.example.keyboardcommands.config.ModConfig;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** BindingManager 按键检测与指令队列逻辑测试。 */
class BindingManagerTest {

	@TempDir
	Path tempDir;

	private TestPlatform platform;
	private ConfigManager configManager;
	private BindingManager bindingManager;

	private void setup(BindingEntry... entries) {
		this.platform = new TestPlatform(this.tempDir);
		this.configManager = new ConfigManager(this.platform);
		this.configManager.setConfig(new ModConfig("key.keyboard.k", Arrays.asList(entries)));
		this.bindingManager = new BindingManager(this.platform, this.configManager);
		this.bindingManager.init();
	}

	private TestPlatform.FakeBoundKey bindingKey(int index) {
		return this.platform.getKey("key.commandkeybind.binding." + index);
	}

	private TestPlatform.FakeBoundKey openMenuKey() {
		return this.platform.getKey("key.commandkeybind.open_menu");
	}

	@Test
	void pressBindingFiresAllCommandsWhenIntervalZero() {
		setup(new BindingEntry("b1", "key.keyboard.g", Arrays.asList("/a", "/b"), 0));
		this.bindingKey(0).press();
		this.bindingManager.onEndTick(true);
		assertEquals(Arrays.asList("/a", "/b"), this.platform.sentCommands);
		assertEquals(2, this.platform.sentMessages.size());
	}

	@Test
	void intervalDelaysCommandsBetweenFires() {
		setup(new BindingEntry("b1", "key.keyboard.g", Arrays.asList("/a", "/b", "/c"), 2));
		this.bindingKey(0).press();

		this.bindingManager.onEndTick(true); // t1 -> 发 /a，等待 2 tick
		assertEquals(Collections.singletonList("/a"), this.platform.sentCommands);
		this.bindingManager.onEndTick(true); // t2 等待
		this.bindingManager.onEndTick(true); // t3 等待
		assertEquals(Collections.singletonList("/a"), this.platform.sentCommands);
		this.bindingManager.onEndTick(true); // t4 -> 发 /b
		assertEquals(Arrays.asList("/a", "/b"), this.platform.sentCommands);
		this.bindingManager.onEndTick(true); // t5
		this.bindingManager.onEndTick(true); // t6
		assertEquals(Arrays.asList("/a", "/b"), this.platform.sentCommands);
		this.bindingManager.onEndTick(true); // t7 -> 发 /c
		assertEquals(Arrays.asList("/a", "/b", "/c"), this.platform.sentCommands);
	}

	@Test
	void unboundKeyIsIgnored() {
		setup(new BindingEntry("b1", "", Arrays.asList("/a"), 0));
		this.bindingKey(0).press();
		this.bindingManager.onEndTick(true);
		assertTrue(this.platform.sentCommands.isEmpty());
	}

	@Test
	void openMenuKeyOpensConfigScreen() {
		setup();
		this.openMenuKey().press();
		this.bindingManager.onEndTick(true);
		assertEquals(1, this.platform.openScreenCount);
		assertTrue(this.platform.sentCommands.isEmpty());
	}

	@Test
	void notInGameIgnoresPresses() {
		setup(new BindingEntry("b1", "key.keyboard.g", Arrays.asList("/a"), 0));
		this.bindingKey(0).press();
		this.bindingManager.onEndTick(false);
		assertTrue(this.platform.sentCommands.isEmpty());
	}

	@Test
	void emptyCommandListFiresNothing() {
		setup(new BindingEntry("b1", "key.keyboard.g", Collections.emptyList(), 0));
		this.bindingKey(0).press();
		this.bindingManager.onEndTick(true);
		assertTrue(this.platform.sentCommands.isEmpty());
	}

	@Test
	void reloadBindingsReusesKeyInstances() {
		setup(new BindingEntry("b1", "key.keyboard.g", Arrays.asList("/a"), 0));
		assertEquals(2, this.platform.createdKeyCount()); // openMenu + 1 绑定

		this.configManager.getConfig().getBindings().add(new BindingEntry("b2", "key.keyboard.h", Arrays.asList("/b"), 0));
		this.bindingManager.reloadBindings();
		assertEquals(3, this.platform.createdKeyCount());

		// 再次 reload 不应新建实例
		this.bindingManager.reloadBindings();
		assertEquals(3, this.platform.createdKeyCount());

		// reload 后绑定 0 仍可触发且内容正确
		this.bindingKey(0).press();
		this.bindingManager.onEndTick(true);
		assertEquals(Collections.singletonList("/a"), this.platform.sentCommands);
	}

	@Test
	void repressingWhileQueuePendingRestartsQueue() {
		setup(new BindingEntry("b1", "key.keyboard.g", Arrays.asList("/a", "/b"), 1));
		this.bindingKey(0).press();
		this.bindingManager.onEndTick(true); // t1 -> /a，等待 1 tick
		this.bindingKey(0).press(); // 队列未结束时再次按下 -> 中止旧队列，从头重发
		this.bindingManager.onEndTick(true); // t2 -> /a（新队列首条）
		assertEquals(Arrays.asList("/a", "/a"), this.platform.sentCommands);
		this.bindingManager.onEndTick(true); // t3 等待
		this.bindingManager.onEndTick(true); // t4 -> /b
		assertEquals(Arrays.asList("/a", "/a", "/b"), this.platform.sentCommands);
	}
}
