package com.example.keyboardcommands.input;

import com.example.keyboardcommands.api.BoundKey;
import com.example.keyboardcommands.api.Platform;
import com.example.keyboardcommands.config.BindingEntry;
import com.example.keyboardcommands.config.ConfigManager;
import com.example.keyboardcommands.config.ModConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * 按键监听与指令执行管理。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>创建"打开配置菜单"按键（注册进原版 Controls 界面）；</li>
 *   <li>根据配置为每个绑定条目创建监听按键；</li>
 *   <li>在客户端 tick 中检测按键并执行指令、发送提示消息；</li>
 *   <li>支持指令按 tick 间隔排队执行（interval=0 立即连续执行）。</li>
 * </ul>
 *
 * <p>本类零 MC 依赖，是否处于游戏中由调用方（各版本模块）判断后传入。</p>
 */
public final class BindingManager {

	public static final String OPEN_MENU_KEY_NAME = "key.commandkeybind.open_menu";

	private final Platform platform;
	private final ConfigManager configManager;

	/** 打开配置菜单按键（进入原版 Controls 界面，键位由原版持久化）。 */
	private BoundKey openMenuKey;

	/** 每个配置条目对应的监听按键。 */
	private final List<BoundKey> bindingKeys = new ArrayList<>();

	/** 正在按间隔排队执行的指令（null 表示空闲）。 */
	private List<String> pendingCommands;
	private int pendingIndex;
	/** 每条指令之间的固定间隔（tick）。 */
	private int pendingInterval;
	/** 距执行下一条还需等待的 tick 数。 */
	private int pendingRemaining;

	public BindingManager(Platform platform, ConfigManager configManager) {
		this.platform = platform;
		this.configManager = configManager;
	}

	/** 初始化：注册打开菜单按键并重建绑定监听按键。 */
	public void init() {
		this.openMenuKey = this.platform.createKey(
			OPEN_MENU_KEY_NAME,
			this.configManager.getConfig().getOpenMenuKey(),
			true);
		this.reloadBindings();
	}

	/** 配置变化后重建全部绑定监听按键。 */
	public void reloadBindings() {
		ModConfig config = this.configManager.getConfig();
		List<BindingEntry> entries = config.getBindings();

		// 复用已有监听按键实例，避免重复创建导致原版静态注册表（ALL/MAP）残留泄漏。
		while (this.bindingKeys.size() < entries.size()) {
			this.bindingKeys.add(this.platform.createKey(
				"key.commandkeybind.binding." + this.bindingKeys.size(),
				"",
				false));
		}
		while (this.bindingKeys.size() > entries.size()) {
			this.bindingKeys.remove(this.bindingKeys.size() - 1);
		}
		for (int i = 0; i < this.bindingKeys.size(); i++) {
			this.bindingKeys.get(i).setKey(entries.get(i).getKey());
		}
	}

	/**
	 * 客户端 tick 时调用：检测按键并推进指令队列。
	 *
	 * @param inGame 是否处于游戏中（有玩家且未打开任何界面），由版本模块判断后传入
	 */
	public void onEndTick(boolean inGame) {
		if (!inGame) {
			return;
		}

		if (this.openMenuKey != null && this.openMenuKey.consumeClick()) {
			this.platform.openConfigScreen();
			return;
		}

		List<BindingEntry> entries = this.configManager.getConfig().getBindings();
		for (int i = 0; i < entries.size() && i < this.bindingKeys.size(); i++) {
			BoundKey key = this.bindingKeys.get(i);
			if (key.consumeClick()) {
				this.queue(entries.get(i));
			}
		}

		this.processPending();
	}

	/** 将某个绑定的指令列表入队执行。 */
	private void queue(BindingEntry entry) {
		List<String> commands = entry.getCommands();
		this.pendingCommands = new ArrayList<>(commands);
		this.pendingIndex = 0;
		this.pendingInterval = entry.getIntervalTicks();
		this.pendingRemaining = 0;
	}

	/** 按间隔推进队列，逐条执行。 */
	private void processPending() {
		if (this.pendingCommands == null) {
			return;
		}

		if (this.pendingRemaining > 0) {
			this.pendingRemaining--;
			return;
		}

		// interval=0 时在同一 tick 内连续执行全部命令；
		// interval>0 时每执行一条后等待 interval 个 tick 再执行下一条。
		while (this.pendingCommands != null && this.pendingRemaining == 0 && this.pendingIndex < this.pendingCommands.size()) {
			String command = this.pendingCommands.get(this.pendingIndex);
			this.platform.sendCommand(command);
			this.platform.sendSystemMessage("commandkeybind.chat.executed", command);
			this.pendingIndex++;

			if (this.pendingIndex >= this.pendingCommands.size()) {
				this.pendingCommands = null;
			} else {
				this.pendingRemaining = this.pendingInterval;
			}
		}
	}
}
