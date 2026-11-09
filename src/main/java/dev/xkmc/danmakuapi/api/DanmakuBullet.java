package dev.xkmc.danmakuapi.api;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.DisplayType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

public interface DanmakuBullet {
	int damage();

	boolean bypass();

	String getName();

	DisplayType display();

	ItemProviderEntry<? extends DanmakuItem> get(DyeColor color);

}
