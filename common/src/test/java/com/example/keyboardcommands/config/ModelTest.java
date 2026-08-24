package com.example.keyboardcommands.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 数据模型构造/深拷贝/容错测试。 */
class ModelTest {

	@Test
	void bindingEntrySanitizesNullsAndNegativeInterval() {
		BindingEntry entry = new BindingEntry(null, null, null, -3);
		assertEquals("", entry.getName());
		assertEquals("", entry.getKey());
		assertTrue(entry.getCommands().isEmpty());
		assertEquals(0, entry.getIntervalTicks());
	}

	@Test
	void bindingEntryCopyIsDeep() {
		BindingEntry entry = new BindingEntry("n", "k", Arrays.asList("/a"), 5);
		BindingEntry copy = entry.copy();
		copy.setName("m");
		copy.getCommands().add("/b");
		copy.setIntervalTicks(9);

		assertEquals("n", entry.getName());
		assertEquals(Arrays.asList("/a"), entry.getCommands());
		assertEquals(5, entry.getIntervalTicks());
	}

	@Test
	void modConfigCopyIsDeep() {
		ModConfig config = new ModConfig("key.keyboard.k",
			Arrays.asList(new BindingEntry("a", "key.keyboard.g", Arrays.asList("/x"), 0)));
		ModConfig copy = config.copy();
		copy.getBindings().get(0).getCommands().add("/y");
		copy.setOpenMenuKey("key.keyboard.h");

		assertEquals("key.keyboard.k", config.getOpenMenuKey());
		assertEquals(1, config.getBindings().get(0).getCommands().size());
	}
}
