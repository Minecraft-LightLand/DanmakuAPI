package dev.xkmc.danmakuapi.content.custom.screen;

import dev.xkmc.danmakuapi.content.custom.data.ISpellFormData;
import dev.xkmc.danmakuapi.content.custom.editor.SpellOptionInstances;
import dev.xkmc.danmakuapi.init.data.DanmakuConfig;
import dev.xkmc.danmakuapi.init.data.DanmakuLang;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class EditorScreen extends OptionsSubScreen {

	private final SpellOptionInstances<?> ins;
	private Button cancel, reset, save;
	private OptionsList list;

	public EditorScreen(Component title, ISpellFormData<?> data) {
		super(null, null, title);
		ins = SpellOptionInstances.create(Wrappers.cast(data));
	}

	@Override
	protected void init() {
		this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
		this.addWidget(this.list);
		this.addOptions();
		int w = width / 4;
		cancel = this.addRenderableWidget(new Button.Builder(
				Component.translatable("gui.cancel"),
				e -> onClose()
		).bounds(width / 10, height - 27, w, 20).build());
		reset = this.addRenderableWidget(new Button.Builder(
				DanmakuLang.EDITOR_RESET.get(),
				e -> reset()
		).bounds(width / 2 - w / 2, height - 27, w, 20).build());
		save = this.addRenderableWidget(new Button.Builder(
				Component.translatable("gui.done"),
				e -> save()
		).bounds(width - width / 10 - w, height - 27, w, 20).build());
	}

	protected void addOptions() {
		ins.add(list, this::setChanged);
		reset.active = false;
	}

	private void setChanged() {
		reset.active = true;
		updateSave();
	}

	private void reset() {
		ins.reset();
		removeWidget(list);
		list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
		addWidget(list);
		addOptions();
		updateSave();
	}

	private void updateSave() {
		var e = ins.build();
		save.setTooltip(null);
		save.active = false;
		if (e != null) {
			int max = DanmakuConfig.SERVER.customSpellMaxDuration.get();
			int dur = e.getDuration();
			if (dur > max) {
				save.setTooltip(Tooltip.create(DanmakuLang.INVALID_TIME.get(max, dur)));
			} else {
				save.active = true;
			}
		}
	}

	private void save() {
		ins.save();
		onClose();
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pTick) {
		renderBackground(g);
		list.render(g, mx, my, pTick);
		super.render(g, mx, my, pTick);
	}

}
