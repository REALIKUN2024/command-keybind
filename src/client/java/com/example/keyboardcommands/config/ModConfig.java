package com.example.keyboardcommands.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 模组配置数据模型。
 *
 * <p>持久化为 config 目录下的 JSON 文件。</p>
 */
public class ModConfig {

	/** 打开配置界面的按键编码（{@code InputConstants.Key#getName()} 格式），默认 K。 */
	private String openMenuKey;

	/** 所有按键绑定条目。 */
	private List<BindingEntry> bindings;

	public ModConfig() {
		this("key.keyboard.k", new ArrayList<>());
	}

	public ModConfig(String openMenuKey, List<BindingEntry> bindings) {
		this.openMenuKey = openMenuKey == null ? "key.keyboard.k" : openMenuKey;
		this.bindings = bindings == null ? new ArrayList<>() : new ArrayList<>(bindings);
	}

	public String getOpenMenuKey() {
		return this.openMenuKey;
	}

	public void setOpenMenuKey(String openMenuKey) {
		this.openMenuKey = openMenuKey == null ? "key.keyboard.k" : openMenuKey;
	}

	public List<BindingEntry> getBindings() {
		return this.bindings;
	}

	public void setBindings(List<BindingEntry> bindings) {
		this.bindings = bindings == null ? new ArrayList<>() : new ArrayList<>(bindings);
	}

	/** 深拷贝副本。 */
	public ModConfig copy() {
		ModConfig copy = new ModConfig(this.openMenuKey, new ArrayList<>());
		for (BindingEntry entry : this.bindings) {
			copy.bindings.add(entry.copy());
		}
		return copy;
	}
}
