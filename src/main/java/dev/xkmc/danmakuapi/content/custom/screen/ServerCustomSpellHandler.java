package dev.xkmc.danmakuapi.content.custom.screen;

import dev.xkmc.danmakuapi.content.custom.annotation.ArgGroup;
import dev.xkmc.danmakuapi.content.custom.data.ISpellFormData;
import dev.xkmc.danmakuapi.init.data.DanmakuConfig;
import dev.xkmc.danmakuapi.init.data.DanmakuTagGen;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ServerCustomSpellHandler {

	public static void handle(@Nullable ServerPlayer sender, Record data) {
		if (sender == null) return;
		ItemStack stack = sender.getMainHandItem();
		if (!stack.is(DanmakuTagGen.CUSTOM_SPELL)) return;
		if (!(data instanceof ISpellFormData<?> form)) return;
		var tag = TagCodec.valueToTag(Record.class, data, e -> true);
		if (tag == null) return;
		int max = DanmakuConfig.SERVER.customSpellMaxDuration.get();
		if (form.getDuration() > max) return;
		var group = ArgGroup.of(data.getClass());
		if (!group.verifySafe(data)) return;
		stack.getOrCreateTag().put("SpellData", tag);
	}

}
