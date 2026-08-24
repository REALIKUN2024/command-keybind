package com.example.keyboardcommands.gui;

import com.example.keyboardcommands.config.BindingEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 1.19.2 指令编辑界面：编辑某个绑定条目下的绑定名称、指令列表与执行间隔。
 */
public class EditCommandsScreen extends Screen {

	private static final int ITEM_HEIGHT = 24;
	private static final int INPUT_HEIGHT = 18;
	private static final int ROW_SPACING = 4;
	private static final int HEADER_HEIGHT = 36;
	private static final int FOOTER_HEIGHT = 36;

	private final Screen lastScreen;
	private final BindingEntry binding;

	private CommandList list;
	private EditBox nameBox;
	private EditBox intervalBox;

	public EditCommandsScreen(Screen lastScreen, BindingEntry binding) {
		super(Component.translatable("commandkeybind.screen.edit.title", binding.getName()));
		this.lastScreen = lastScreen;
		this.binding = binding;
	}

	@Override
	protected void init() {
		this.nameBox = new EditBox(this.font, 0, 0, 200, INPUT_HEIGHT, Component.empty());
		this.nameBox.setValue(this.binding.getName());
		this.nameBox.setMaxLength(64);
		this.nameBox.setResponder(value -> this.binding.setName(value));
		this.addRenderableWidget(this.nameBox);

		this.list = new CommandList(this.minecraft, this);
		this.addRenderableWidget(this.list);
		// 关闭列表顶部/底部渐隐遮罩，避免遮罩盖住标题、名称输入框等外部元素
		this.list.setRenderTopAndBottom(false);

		this.intervalBox = new EditBox(this.font, 0, 0, 80, INPUT_HEIGHT, Component.empty());
		this.intervalBox.setValue(Integer.toString(this.binding.getIntervalTicks()));
		this.intervalBox.setMaxLength(6);
		this.intervalBox.setResponder(value -> this.binding.setIntervalTicks(this.parseInterval(value)));
		this.addRenderableWidget(this.intervalBox);

		int footerY = this.height - FOOTER_HEIGHT;
		this.addRenderableWidget(new Button(this.width / 2 - 110, footerY + 8, 100, 20,
			Component.translatable("commandkeybind.screen.add_command"), button -> {
				this.binding.getCommands().add("");
				this.list.refreshEntries();
			}));
		this.addRenderableWidget(new Button(this.width / 2 + 10, footerY + 8, 100, 20,
			CommonComponents.GUI_DONE, button -> this.onClose()));

		this.repositionElements();
	}

	private void repositionElements() {
		int inputY1 = HEADER_HEIGHT + ROW_SPACING;
		int inputY2 = this.height - FOOTER_HEIGHT - ROW_SPACING - INPUT_HEIGHT;

		int gap = 6;
		int nameTotal = this.font.width(Component.translatable("commandkeybind.screen.name_label")) + gap + this.nameBox.getWidth();
		this.nameBox.x = this.width / 2 - nameTotal / 2 + this.font.width(Component.translatable("commandkeybind.screen.name_label")) + gap;
				this.nameBox.y = inputY1;
		int intervalTotal = this.font.width(Component.translatable("commandkeybind.screen.interval_label")) + gap + this.intervalBox.getWidth();
		this.intervalBox.x = this.width / 2 - intervalTotal / 2 + this.font.width(Component.translatable("commandkeybind.screen.interval_label")) + gap;
				this.intervalBox.y = inputY2;

		int listY = inputY1 + INPUT_HEIGHT + ROW_SPACING;
		// 1.16~1.20.1 的 updateSize(int width, int height, int y0, int y1)：第 4 参是列表底部坐标，不是 itemHeight！
		this.list.updateSize(this.width, this.height, listY, inputY2 - ROW_SPACING);
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
		// 半透明黑渐变背景（原版游戏内界面风格），避免 renderBackground 在主菜单时拉伸泥土纹理
		this.fillGradient(poseStack, 0, 0, this.width, this.height, -1072689136, -804253680);
		GuiComponent.drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 0xFFFFFF);
		super.render(poseStack, mouseX, mouseY, delta);
		GuiComponent.drawString(poseStack, this.font,
			Component.translatable("commandkeybind.screen.name_label"),
			this.width / 2 - (this.font.width(Component.translatable("commandkeybind.screen.name_label")) + 6 + this.nameBox.getWidth()) / 2,
			HEADER_HEIGHT + ROW_SPACING + (INPUT_HEIGHT - 9) / 2, 0xFFFFFF);
		GuiComponent.drawString(poseStack, this.font,
			Component.translatable("commandkeybind.screen.interval_label"),
			this.width / 2 - (this.font.width(Component.translatable("commandkeybind.screen.interval_label")) + 6 + this.intervalBox.getWidth()) / 2,
			this.height - FOOTER_HEIGHT - ROW_SPACING - INPUT_HEIGHT + (INPUT_HEIGHT - 9) / 2, 0xFFFFFF);
	}

	@Override
	public void onClose() {
		if (this.lastScreen instanceof ConfigScreen configScreen) {
			configScreen.refreshEntries();
		}
		this.minecraft.setScreen(this.lastScreen);
	}

	/** 解析间隔输入，非法或负数时回退为 0。 */
	private static int parseInterval(String value) {
		try {
			return Math.max(0, Integer.parseInt(value.trim()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static final class CommandList extends ContainerObjectSelectionList<CommandList.Entry> {
		private final EditCommandsScreen screen;

		private CommandList(Minecraft minecraft, EditCommandsScreen screen) {
			super(minecraft, screen.width, screen.height, 0, 0, ITEM_HEIGHT);
			this.screen = screen;
			this.refreshEntries();
		}

		private void refreshEntries() {
			this.clearEntries();
			List<String> commands = this.screen.binding.getCommands();
			for (int i = 0; i < commands.size(); i++) {
				this.addEntry(new Entry(i));
			}
		}

		@Override
		public int getRowWidth() {
			return 340;
		}

		private final class Entry extends ContainerObjectSelectionList.Entry<Entry> {
			private final int index;
			private final EditBox commandBox;
			private final Button deleteButton;

			private Entry(int index) {
				this.index = index;
				this.commandBox = new EditBox(CommandList.this.minecraft.font, 0, 0, 240, 20, Component.literal("command"));
				this.commandBox.setValue(CommandList.this.screen.binding.getCommands().get(index));
				this.commandBox.setMaxLength(256);
				this.commandBox.setResponder(value -> CommandList.this.screen.binding.getCommands().set(this.index, value));
				this.deleteButton = new Button(0, 0, 40, 20, Component.translatable("commandkeybind.screen.delete"), button -> {
					CommandList.this.screen.binding.getCommands().remove(this.index);
					CommandList.this.refreshEntries();
				});
			}

			@Override
			public void render(PoseStack poseStack, int index, int y, int rowLeft, int rowWidth, int itemHeight, int mouseX, int mouseY, boolean hovered, float delta) {
				int rowY = y - 2;
				this.commandBox.x = rowLeft;
				this.commandBox.y = rowY;
				this.commandBox.render(poseStack, mouseX, mouseY, delta);
				int bx = CommandList.this.getScrollbarPosition() - 6 - this.deleteButton.getWidth();
				this.deleteButton.x = bx;
				this.deleteButton.y = rowY;
				this.deleteButton.render(poseStack, mouseX, mouseY, delta);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.commandBox, this.deleteButton);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.commandBox, this.deleteButton);
			}
		}
	}
}
