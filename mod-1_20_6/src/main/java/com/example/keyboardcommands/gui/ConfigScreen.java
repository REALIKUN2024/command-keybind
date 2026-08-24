package com.example.keyboardcommands.gui;

import com.example.keyboardcommands.KeyboardCommandsClient;
import com.example.keyboardcommands.config.BindingEntry;
import com.example.keyboardcommands.config.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 1.21.8 主配置界面：列出所有按键绑定条目，支持添加、删除、改键、编辑指令。
 */
public class ConfigScreen extends Screen {

	private static final Component TITLE = Component.translatable("commandkeybind.screen.title");
	private static final int ITEM_HEIGHT = 24;

	private final Screen lastScreen;
	private final ConfigManager configManager;

	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private BindingList list;

	/** 正在等待捕获新按键的条目；null 表示未处于改键模式。 */
	private BindingEntry selectedKeyBinding;

	public ConfigScreen(Screen lastScreen) {
		super(TITLE);
		this.lastScreen = lastScreen;
		this.configManager = KeyboardCommandsClient.CONFIG_MANAGER;
	}

	@Override
	protected void init() {
		this.layout.addTitleHeader(this.title, this.font);
		this.list = this.layout.addToContents(new BindingList(this.minecraft, this));
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(Component.translatable("commandkeybind.screen.add_binding"), button -> {
			int index = this.configManager.getConfig().getBindings().size() + 1;
			this.configManager.getConfig().getBindings().add(new BindingEntry(
				Component.translatable("commandkeybind.screen.binding_name", index).getString(), "", new ArrayList<>()));
			this.list.refreshEntries();
		}).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).build());
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		this.list.updateSize(this.width, this.layout);
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
			super(minecraft, screen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), ITEM_HEIGHT);
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
			// 与 1.16~1.20.1 一致：固定行宽，行内左侧名称、右侧按钮组
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
			public void render(GuiGraphics graphics, int index, int y, int rowLeft, int rowWidth, int itemHeight, int mouseX, int mouseY, boolean hovered, float delta) {
				// 与 26.1 布局一致：名称在行左，按钮组右缘 = 行右缘 - 2（等效 26.1 的 scrollBarX() - 10）
				this.nameWidget.setPosition(rowLeft, y - 2);
				int rightEdge = rowLeft + rowWidth - 2;
				int bx = rightEdge;
				bx -= this.deleteButton.getWidth();
				this.deleteButton.setPosition(bx, y - 2);
				bx -= 5 + this.editButton.getWidth();
				this.editButton.setPosition(bx, y - 2);
				bx -= 5 + this.changeButton.getWidth();
				this.changeButton.setPosition(bx, y - 2);
				this.nameWidget.render(graphics, mouseX, mouseY, delta);
				this.deleteButton.render(graphics, mouseX, mouseY, delta);
				this.editButton.render(graphics, mouseX, mouseY, delta);
				this.changeButton.render(graphics, mouseX, mouseY, delta);
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
