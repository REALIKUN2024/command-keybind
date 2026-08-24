package com.example.keyboardcommands.gui;

import com.example.keyboardcommands.KeyboardCommandsClient;
import com.example.keyboardcommands.config.BindingEntry;
import com.example.keyboardcommands.config.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 1.20.1 主配置界面：列出所有按键绑定条目，支持添加、删除、改键、编辑指令。
 *
 * <p>1.20.1 布局 API 较旧，采用手工布局。</p>
 */
public class ConfigScreen extends Screen {

	private static final Component TITLE = Component.translatable("commandkeybind.screen.title");
	private static final int ITEM_HEIGHT = 24;
	private static final int HEADER_HEIGHT = 36;
	private static final int FOOTER_HEIGHT = 36;

	private final Screen lastScreen;
	private final ConfigManager configManager;

	private BindingList list;
	private StringWidget titleWidget;

	/** 正在等待捕获新按键的条目；null 表示未处于改键模式。 */
	private BindingEntry selectedKeyBinding;

	public ConfigScreen(Screen lastScreen) {
		super(TITLE);
		this.lastScreen = lastScreen;
		this.configManager = KeyboardCommandsClient.CONFIG_MANAGER;
	}

	@Override
	protected void init() {
		this.titleWidget = new StringWidget(this.width / 2 - 100, 6, 200, 20, this.title, this.font);
		this.addRenderableWidget(this.titleWidget);

		this.list = new BindingList(this.minecraft, this);
		this.addRenderableWidget(this.list);
		// 关闭列表顶部/底部渐隐遮罩，避免遮罩盖住标题、名称输入框等外部元素
		this.list.setRenderTopAndBottom(false);

		int footerY = this.height - FOOTER_HEIGHT;
		this.addRenderableWidget(Button.builder(Component.translatable("commandkeybind.screen.add_binding"), button -> {
			int index = this.configManager.getConfig().getBindings().size() + 1;
			this.configManager.getConfig().getBindings().add(new BindingEntry(
				Component.translatable("commandkeybind.screen.binding_name", index).getString(), "", new ArrayList<>()));
			this.list.refreshEntries();
		}).bounds(this.width / 2 - 110, footerY + 8, 100, 20).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(this.width / 2 + 10, footerY + 8, 100, 20).build());

		this.repositionElements();
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
		// 半透明黑渐变背景（原版游戏内界面风格）
		this.fillGradient(poseStack, 0, 0, this.width, this.height, -1072689136, -804253680);
		super.render(poseStack, mouseX, mouseY, delta);
	}

	@Override
	protected void repositionElements() {
		// 1.16~1.20.1 的 updateSize(int width, int height, int y0, int y1)：第 4 参是列表底部坐标，不是 itemHeight！
		this.list.updateSize(this.width, this.height, HEADER_HEIGHT, this.height - FOOTER_HEIGHT);
	}

	@Override
	public boolean keyPressed(int keyCode, int scancode, int modifiers) {
		if (this.selectedKeyBinding != null) {
			if (keyCode == 256) { // GLFW escape
				this.selectedKeyBinding.setKey("");
			} else {
				this.selectedKeyBinding.setKey(InputConstants.getKey(keyCode, scancode).getName());
			}
			this.selectedKeyBinding = null;
			this.list.refreshEntries();
			return true;
		}
		return super.keyPressed(keyCode, scancode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.selectedKeyBinding != null) {
			this.selectedKeyBinding.setKey(InputConstants.Type.MOUSE.getOrCreate(button).getName());
			this.selectedKeyBinding = null;
			this.list.refreshEntries();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void onClose() {
		this.configManager.save();
		KeyboardCommandsClient.BINDING_MANAGER.reloadBindings();
		this.minecraft.setScreen(this.lastScreen);
	}

	/** 删除绑定条目。 */
	private void deleteBinding(BindingEntry entry) {
		this.configManager.getConfig().getBindings().remove(entry);
		this.list.refreshEntries();
	}

	/** 子页面返回时刷新列表（如重命名绑定后）。 */
	public void refreshEntries() {
		this.list.refreshEntries();
	}

	private void startCapture(BindingEntry entry) {
		this.selectedKeyBinding = entry;
		this.list.refreshEntries();
	}

	private static final class BindingList extends ContainerObjectSelectionList<BindingList.Entry> {
		private final ConfigScreen screen;

		private BindingList(Minecraft minecraft, ConfigScreen screen) {
			super(minecraft, screen.width, screen.height, 0, 0, ITEM_HEIGHT);
			this.screen = screen;
			this.refreshEntries();
		}

		private void refreshEntries() {
			this.clearEntries();
			List<BindingEntry> bindings = this.screen.configManager.getConfig().getBindings();
			for (BindingEntry binding : bindings) {
				this.addEntry(new Entry(binding));
			}
		}

		@Override
		public int getRowWidth() {
			return 340;
		}

		private final class Entry extends ContainerObjectSelectionList.Entry<Entry> {
			private final BindingEntry binding;
			private final StringWidget nameWidget;
			private final Button changeButton;
			private final Button editButton;
			private final Button deleteButton;

			private Entry(BindingEntry binding) {
				this.binding = binding;
				this.nameWidget = new StringWidget(120, 20, Component.literal(binding.getName()), BindingList.this.minecraft.font);
				this.changeButton = Button.builder(Component.literal(binding.getKey()), button -> {
					BindingList.this.screen.startCapture(binding);
				}).bounds(0, 0, 90, 20).build();
				this.editButton = Button.builder(Component.translatable("commandkeybind.screen.edit"), button -> {
					BindingList.this.screen.selectedKeyBinding = null;
					BindingList.this.minecraft.setScreen(new EditCommandsScreen(BindingList.this.screen, binding));
				}).bounds(0, 0, 50, 20).build();
				this.deleteButton = Button.builder(Component.translatable("commandkeybind.screen.delete"), button -> {
					BindingList.this.screen.deleteBinding(binding);
				}).bounds(0, 0, 40, 20).build();
				this.refreshEntry();
			}

			private void refreshEntry() {
				this.nameWidget.setMessage(Component.literal(this.binding.getName()));
				String keyName = this.binding.getKey();
				Component keyComponent;
				if (keyName.isEmpty()) {
					keyComponent = Component.translatable("commandkeybind.screen.unbound");
				} else {
					try {
						keyComponent = InputConstants.getKey(keyName).getDisplayName();
					} catch (IllegalArgumentException e) {
						keyComponent = Component.literal(keyName);
					}
				}
				if (BindingList.this.screen.selectedKeyBinding == this.binding) {
					this.changeButton.setMessage(Component.translatable("commandkeybind.screen.capture"));
				} else {
					this.changeButton.setMessage(keyComponent);
				}
			}

			@Override
			public void render(PoseStack poseStack, int index, int y, int rowLeft, int rowWidth, int itemHeight, int mouseX, int mouseY, boolean hovered, float delta) {
				this.nameWidget.setPosition(rowLeft, y - 2);
				this.nameWidget.render(poseStack, mouseX, mouseY, delta);
				int rightEdge = BindingList.this.getScrollbarPosition() - 6;
				int bx = rightEdge;
				bx -= this.deleteButton.getWidth();
				this.deleteButton.setPosition(bx, y - 2);
				bx -= 5 + this.editButton.getWidth();
				this.editButton.setPosition(bx, y - 2);
				bx -= 5 + this.changeButton.getWidth();
				this.changeButton.setPosition(bx, y - 2);
				this.deleteButton.render(poseStack, mouseX, mouseY, delta);
				this.editButton.render(poseStack, mouseX, mouseY, delta);
				this.changeButton.render(poseStack, mouseX, mouseY, delta);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.nameWidget, this.changeButton, this.editButton, this.deleteButton);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.nameWidget, this.changeButton, this.editButton, this.deleteButton);
			}
		}
	}
}
