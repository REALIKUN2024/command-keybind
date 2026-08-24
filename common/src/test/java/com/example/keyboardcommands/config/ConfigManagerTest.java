package com.example.keyboardcommands.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.keyboardcommands.TestPlatform;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** ConfigManager 读写、容错与默认值测试。 */
class ConfigManagerTest {

	@TempDir
	Path tempDir;

	private ConfigManager newManager() {
		return new ConfigManager(new TestPlatform(this.tempDir));
	}

	@Test
	void loadCreatesDefaultConfigWhenMissing() throws Exception {
		ConfigManager cm = this.newManager();
		cm.load();
		assertTrue(Files.exists(this.tempDir.resolve("command-keybind.json")));
		assertEquals("key.keyboard.k", cm.getConfig().getOpenMenuKey());
		assertTrue(cm.getConfig().getBindings().isEmpty());
	}

	@Test
	void saveThenLoadRoundTrips() {
		ConfigManager cm = this.newManager();
		cm.load();
		ModConfig config = cm.getConfig();
		config.setOpenMenuKey("key.keyboard.h");
		config.getBindings().add(new BindingEntry("创造模式", "key.keyboard.g",
			Arrays.asList("/gamemode creative", "/say hi"), 3));
		cm.save();

		ConfigManager cm2 = this.newManager();
		cm2.load();
		ModConfig loaded = cm2.getConfig();
		assertEquals("key.keyboard.h", loaded.getOpenMenuKey());
		assertEquals(1, loaded.getBindings().size());
		BindingEntry entry = loaded.getBindings().get(0);
		assertEquals("创造模式", entry.getName());
		assertEquals("key.keyboard.g", entry.getKey());
		assertEquals(Arrays.asList("/gamemode creative", "/say hi"), entry.getCommands());
		assertEquals(3, entry.getIntervalTicks());
	}

	@Test
	void corruptedJsonFallsBackToDefault() throws Exception {
		Files.write(this.tempDir.resolve("command-keybind.json"),
			"{ not valid json !!!".getBytes(StandardCharsets.UTF_8));
		ConfigManager cm = this.newManager();
		cm.load(); // 不应抛异常
		assertEquals("key.keyboard.k", cm.getConfig().getOpenMenuKey());
	}

	@Test
	void nullTopLevelFieldsAreSanitized() throws Exception {
		Files.write(this.tempDir.resolve("command-keybind.json"),
			"{\"openMenuKey\": null, \"bindings\": null}".getBytes(StandardCharsets.UTF_8));
		ConfigManager cm = this.newManager();
		cm.load();
		assertEquals("key.keyboard.k", cm.getConfig().getOpenMenuKey());
		assertTrue(cm.getConfig().getBindings().isEmpty());
	}

	@Test
	void nullEntryFieldsAreSanitized() throws Exception {
		Files.write(this.tempDir.resolve("command-keybind.json"),
			("{\"openMenuKey\":\"key.keyboard.k\",\"bindings\":[{\"name\":null,\"key\":null,\"commands\":null,\"intervalTicks\":-5}]}")
				.getBytes(StandardCharsets.UTF_8));
		ConfigManager cm = this.newManager();
		cm.load();
		BindingEntry entry = cm.getConfig().getBindings().get(0);
		assertEquals("", entry.getName());
		assertEquals("", entry.getKey());
		assertTrue(entry.getCommands().isEmpty());
		assertEquals(0, entry.getIntervalTicks());
	}
}
