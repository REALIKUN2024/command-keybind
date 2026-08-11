package com.example.keyboardcommands.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个"按键-指令"绑定条目。
 */
public class BindingEntry {

	/** 绑定名称（显示用）。 */
	private String name;

	/** 触发按键编码（{@code InputConstants.Key#getName()} 格式），空串表示未绑定。 */
	private String key;

	/** 按下该键时依次执行的指令列表（可含前导 '/'）。 */
	private List<String> commands;

	/** 每条指令之间的执行间隔（单位：tick），0 表示立即连续执行。 */
	private int intervalTicks;

	public BindingEntry() {
		this("", "", new ArrayList<>(), 0);
	}

	public BindingEntry(String name, String key, List<String> commands) {
		this(name, key, commands, 0);
	}

	public BindingEntry(String name, String key, List<String> commands, int intervalTicks) {
		this.name = name == null ? "" : name;
		this.key = key == null ? "" : key;
		this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
		this.intervalTicks = Math.max(0, intervalTicks);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name == null ? "" : name;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key == null ? "" : key;
	}

	public List<String> getCommands() {
		return this.commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
	}

	public int getIntervalTicks() {
		return this.intervalTicks;
	}

	public void setIntervalTicks(int intervalTicks) {
		this.intervalTicks = Math.max(0, intervalTicks);
	}

	/** 深拷贝副本。 */
	public BindingEntry copy() {
		return new BindingEntry(this.name, this.key, this.commands, this.intervalTicks);
	}
}
