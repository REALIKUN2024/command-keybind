package com.example.keyboardcommands.gui;

import com.example.keyboardcommands.KeyboardCommandsClient;
import com.example.keyboardcommands.config.BindingEntry;
import com.example.keyboardcommands.config.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * 1.19.2 主配置界面：列出所有按键绑定条目，支持添加、删除、改键、编辑指令。
 *
 * <p>1.19.2 无 StringWidget，文字用 GuiComponent.drawString 绘制；Button 用构造式。</p>
 */
public class ConfigScreen extends Screen {

	private static final Component TITLE = new TranslatableComponent("commandkeybind.screen.title");
	private static final int ITEM_HEIGHT = 24;
	private static final int HEADER_HEIGHT = 36;
	private static final int FOOTER_HEIGHT = 36;

	private final Screen lastScreen;
	private final ConfigManager configManager;

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
		this.list = new BindingList(this.minecraft, this);
		this.addRenderableWidget(this.list);

		int footerY = this.height - FOOTER_HEIGHT;
		this.addRenderableWidget(new Button(this.width / 2 - 110, footerY + 8, 100, 20,
			new TranslatableComponent("commandkeybind.screen.add_binding"), button -> {
				int index = this.configManager.getConfig().getBindings().size() + 1;
				this.configManager.getConfig().getBindings().add(new BindingEntry(
					new TranslatableComponent("commandkeybind.screen.binding_name", index).getString(), "", new ArrayList<>()));
				this.list.refreshEntries();
			}));
		this.addRenderableWidget(new Button(this.width / 2 + 10, footerY + 8, 100, 20,
			CommonComponents.GUI_DONE, button -> this.onClose()));

		this.repositionElements();
	}

	private void repositionElements() {
		this.list.updateSize(this.width, this.height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ITEM_HEIGHT);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
		this.renderBackground(poseStack);
		GuiComponent.drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 0xFFFFFF);
		super.render(poseStack, mouseX, mouseY, delta);
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
			private final Button changeButton;
			private final Button editButton;
			private final Button deleteButton;

			private Entry(BindingEntry binding) {
				this.binding = binding;
				this.changeButton = new Button(0, 0, 90, 20, new TextComponent(binding.getKey()), button -> {
					BindingList.this.screen.startCapture(binding);
				});
				this.editButton = new Button(0, 0, 50, 20, new TranslatableComponent("commandkeybind.screen.edit"), button -> {
					BindingList.this.screen.selectedKeyBinding = null;
					BindingList.this.minecraft.setScreen(new EditCommandsScreen(BindingList.this.screen, binding));
				});
				this.deleteButton = new Button(0, 0, 40, 20, new TranslatableComponent("commandkeybind.screen.delete"), button -> {
					BindingList.this.screen.deleteBinding(binding);
				});
				this.refreshEntry();
			}

			private void refreshEntry() {
				String keyName = this.binding.getKey();
				Component keyComponent;
				if (keyName.isEmpty()) {
					keyComponent = new TranslatableComponent("commandkeybind.screen.unbound");
				} else {
					try {
						keyComponent = InputConstants.getKey(keyName).getDisplayName();
					} catch (IllegalArgumentException e) {
						keyComponent = new TextComponent(keyName);
					}
				}
				if (BindingList.this.screen.selectedKeyBinding == this.binding) {
					this.changeButton.setMessage(new TranslatableComponent("commandkeybind.screen.capture"));
				} else {
					this.changeButton.setMessage(keyComponent);
				}
			}

			@Override
			public void render(PoseStack poseStack, int index, int y, int width, int itemHeight, int rowLeft, int mouseX, int mouseY, boolean hovered, float delta) {
				GuiComponent.drawString(poseStack, BindingList.this.minecraft.font,
					new TextComponent(this.binding.getName()), rowLeft, y, 0xFFFFFF);
				int rightEdge = rowLeft + width - 10;
				int bx = rightEdge;
				bx -= this.deleteButton.getWidth();
				this.deleteButton.x = bx;
				this.deleteButton.y = y - 2;
				bx -= 5 + this.editButton.getWidth();
				this.editButton.x = bx;
				this.editButton.y = y - 2;
				bx -= 5 + this.changeButton.getWidth();
				this.changeButton.x = bx;
				this.changeButton.y = y - 2;
				this.deleteButton.render(poseStack, mouseX, mouseY, delta);
				this.editButton.render(poseStack, mouseX, mouseY, delta);
				this.changeButton.render(poseStack, mouseX, mouseY, delta);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.changeButton, this.editButton, this.deleteButton);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.changeButton, this.editButton, this.deleteButton);
			}
		}
	}
}
