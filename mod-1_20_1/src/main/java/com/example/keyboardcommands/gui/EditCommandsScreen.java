package com.example.keyboardcommands.gui;

import com.example.keyboardcommands.config.BindingEntry;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 1.20.1 指令编辑界面：编辑某个绑定条目下的绑定名称、指令列表与执行间隔。
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
	private StringWidget nameLabel;
	private EditBox nameBox;
	private StringWidget intervalLabel;
	private EditBox intervalBox;

	public EditCommandsScreen(Screen lastScreen, BindingEntry binding) {
		super(Component.translatable("commandkeybind.screen.edit.title", binding.getName()));
		this.lastScreen = lastScreen;
		this.binding = binding;
	}

	@Override
	protected void init() {
		this.addRenderableWidget(new StringWidget(this.width / 2 - 100, 6, 200, 20, this.title, this.font));

		this.nameLabel = new StringWidget(Component.translatable("commandkeybind.screen.name_label"), this.font);
		this.addRenderableWidget(this.nameLabel);

		this.nameBox = new EditBox(this.font, 0, 0, 200, INPUT_HEIGHT, Component.empty());
		this.nameBox.setHint(Component.translatable("commandkeybind.screen.name_hint"));
		this.nameBox.setValue(this.binding.getName());
		this.nameBox.setMaxLength(64);
		this.nameBox.setResponder(value -> this.binding.setName(value));
		this.addRenderableWidget(this.nameBox);

		this.list = new CommandList(this.minecraft, this);
		this.addRenderableWidget(this.list);

		this.intervalLabel = new StringWidget(Component.translatable("commandkeybind.screen.interval_label"), this.font);
		this.addRenderableWidget(this.intervalLabel);

		this.intervalBox = new EditBox(this.font, 0, 0, 80, INPUT_HEIGHT, Component.empty());
		this.intervalBox.setHint(Component.translatable("commandkeybind.screen.interval_hint"));
		this.intervalBox.setValue(Integer.toString(this.binding.getIntervalTicks()));
		this.intervalBox.setMaxLength(6);
		this.intervalBox.setResponder(value -> this.binding.setIntervalTicks(this.parseInterval(value)));
		this.addRenderableWidget(this.intervalBox);

		int footerY = this.height - FOOTER_HEIGHT;
		this.addRenderableWidget(Button.builder(Component.translatable("commandkeybind.screen.add_command"), button -> {
			this.binding.getCommands().add("");
			this.list.refreshEntries();
		}).bounds(this.width / 2 - 110, footerY + 8, 100, 20).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(this.width / 2 + 10, footerY + 8, 100, 20).build());

		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		int inputY1 = HEADER_HEIGHT + ROW_SPACING;
		int inputY2 = this.height - FOOTER_HEIGHT - ROW_SPACING - INPUT_HEIGHT;

		this.layoutRow(this.nameLabel, this.nameBox, inputY1);
		this.layoutRow(this.intervalLabel, this.intervalBox, inputY2);

		int listY = inputY1 + INPUT_HEIGHT + ROW_SPACING;
		int listHeight = Math.max(ITEM_HEIGHT, inputY2 - listY - ROW_SPACING);
		this.list.updateSize(this.width, listHeight, listY, ITEM_HEIGHT);
	}

	/** 将"标签 + 输入框"作为整体水平居中放置。 */
	private void layoutRow(StringWidget label, EditBox box, int y) {
		int gap = 6;
		int totalWidth = label.getWidth() + gap + box.getWidth();
		int startX = this.width / 2 - totalWidth / 2;
		int labelX = startX;
		int boxX = labelX + label.getWidth() + gap;
		label.setPosition(labelX, y + (INPUT_HEIGHT - 9) / 2);
		box.setPosition(boxX, y);
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
				this.deleteButton = Button.builder(Component.translatable("commandkeybind.screen.delete"), button -> {
					CommandList.this.screen.binding.getCommands().remove(this.index);
					CommandList.this.refreshEntries();
				}).bounds(0, 0, 40, 20).build();
			}

			@Override
			public void render(GuiGraphics graphics, int index, int y, int width, int itemHeight, int rowLeft, int mouseX, int mouseY, boolean hovered, float delta) {
				int rowY = y - 2;
				this.commandBox.setPosition(rowLeft, rowY);
				this.commandBox.render(graphics, mouseX, mouseY, delta);
				int bx = rowLeft + width - 10 - this.deleteButton.getWidth();
				this.deleteButton.setPosition(bx, rowY);
				this.deleteButton.render(graphics, mouseX, mouseY, delta);
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
